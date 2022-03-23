package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.wurstclient.Category;
import net.wurstclient.commands.GoToCmd;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.BlockUtils;
import org.apache.commons.lang3.NotImplementedException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AutoCraftHack extends Hack implements UpdateListener {

    private HashMap<Identifier, List<CraftingProcess>> processMap = null;

    private List<CraftingQueueEntry> craftingQueue = new ArrayList<>();

    private boolean isCurrentlyCrafting = false;

    private ReentrantLock slotUpdateLock = new ReentrantLock();
    private Condition slotUpdateCondition = slotUpdateLock.newCondition();

    private ReentrantLock containerOpenLock = new ReentrantLock();
    private Condition containerOpenCondition = containerOpenLock.newCondition();

    private SlotUpdateInfo latestSlotUpdate;

    private InventoryStorageQuery inventoryQuery = new InventoryStorageQuery();
    private ContainerStorageQuery containerQuery = new ContainerStorageQuery();
    private WorldStorageQuery worldQuery = new WorldStorageQuery();

    private HashMap<Item, Integer> inventoryAvailabilityMap = new HashMap<>();
    private HashMap<Item, Integer> storageAvailabilityMap = new HashMap<>();
    private HashMap<Block, Integer> worldAvailabilityMap = new HashMap<>();
    private HashMap<Block, BlockPos> nearestBlockPosMap = new HashMap<>();
    private HashMap<Block, Double> nearestBlockDistanceMap = new HashMap<>();
    private HashMap<Item, Integer> totalInventoryAvailabilityMap = new HashMap<>();

    private HashSet<Block> containerBlockTypes;

    private Pathfinder pathFinder;
    private BaritoneInterface baritoneInterface;

    private BlockPos latestBlockPos = BlockPos.ORIGIN;

    private boolean doneCrafting = true;

    private ContainerManager containerManager = new ContainerManager();
    private ToolManager toolManager = new ToolManager();
    private BlockManager blockManager = new BlockManager();
    private InventoryManager inventoryManager = new InventoryManager();

    BigInteger globalTimeTaken = BigInteger.ZERO;

    public AutoCraftHack() {
        super("AutoCraft");
        setCategory(Category.ITEMS);
    }

    private boolean isBaritoneAPIInstalled() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        }
        catch (Exception ex) {
            return false;
        }
    }

    private void initContainerBlockTypes() {
        containerBlockTypes = new HashSet<>();
        List<String> blockTypes = List.of("crafting_table", "chest", "furnace");
        for (String type : blockTypes) {
            containerBlockTypes.add(Registry.BLOCK.get(new Identifier("minecraft", type)));
        }
    }

    private CraftingProcess getCraftingProcessByType(Recipe<?> recipe, RecipeType type) {
        if (type == RecipeType.CRAFTING) {
            return new RecipeCraftingProcess(recipe);
        }
        else if (type == RecipeType.SMELTING) {
            return new SmeltingCraftingProcess(recipe);
        }
        return null;
    }

    private String getRecipeNameSeparator(RecipeType type) {
        if (type == RecipeType.CRAFTING) {
            return "_from_";
        }
        else if (type == RecipeType.SMELTING) {
            return "_from_smelting_";
        }
        return null;
    }

    private void initProcessMap() {
        processMap = new HashMap<>();
        RecipeManager recipeManager = MC.world.getRecipeManager();
        Stream<Identifier> keys = recipeManager.keys();
        Iterator<Identifier> iter = keys.iterator();
        while (iter.hasNext()) {
            Identifier id = iter.next();
            Optional<? extends Recipe<?>> optionalRecipe = recipeManager.get(id);
            if (!optionalRecipe.isPresent())
                continue;
            Recipe recipe = optionalRecipe.get();
            RecipeType<?> recipeType = recipe.getType();
            if (!(recipeType == RecipeType.CRAFTING || recipeType == RecipeType.SMELTING))
                continue;
            String path = id.getPath();
            String separator = getRecipeNameSeparator(recipeType);
            if (path.contains(separator)) {
                String[] components = path.split(separator);
                String sourceId = components[0];
                Identifier baseId = new Identifier(id.getNamespace(), sourceId);
                if (!processMap.containsKey(baseId))
                    processMap.put(baseId, new ArrayList<>());
                processMap.get(baseId).add(getCraftingProcessByType(recipe, recipeType));
            }
            else {
                if (!processMap.containsKey(id))
                    processMap.put(id, new ArrayList<>());
                processMap.get(id).add(getCraftingProcessByType(recipe, recipeType));
            }
        }
        for (Identifier id : Registry.BLOCK.getIds()) {
            List<ItemStack> droppedStacks = Block.getDroppedStacks(Registry.BLOCK.get(id).getDefaultState(), MC.getServer().getOverworld(), BlockPos.ORIGIN, null);
            for (ItemStack stack : droppedStacks) {
                Identifier stackId = Registry.ITEM.getId(stack.getItem());
                if (!processMap.containsKey(stackId))
                    processMap.put(stackId, new ArrayList<>());
                processMap.get(stackId).add(new WorldCraftingProcess(stack.getItem(), Registry.BLOCK.get(id)));
            }
        }
    }

    private class ToolManager {
        private List<Item> tools;
        public ToolManager() {
            tools = new ArrayList<>();
            String[] prefixes = new String[] {"wooden", "stone", "iron", "golden", "diamond"};
            String[] suffixes = new String[] {"sword", "axe", "pickaxe", "shovel", "hoe"};
            for (String prefix : prefixes) {
                for (String suffix : suffixes) {
                    tools.add(Registry.ITEM.get(new Identifier("minecraft", prefix + "_" + suffix)));
                }
            }
            tools.add(Registry.ITEM.get(new Identifier("minecraft", "shears")));
        }
        public Set<Item> getMatchingTools(Block block) {
            Set<Item> res = new HashSet<>();
            BlockState state = block.getDefaultState();
            if (!state.isToolRequired())
                return res;
            for (Item tool : tools) {
                ItemStack toolStack = new ItemStack(tool, 1);
                if (toolStack.isSuitableFor(state)) {
                    res.add(tool);
                }
            }
            return res;
        }
        public boolean canMine(Block block) {
            Set<Item> effectiveTools = getMatchingTools(block);
            synchronized (totalInventoryAvailabilityMap) {
                for (Item item : totalInventoryAvailabilityMap.keySet()) {
                    if (effectiveTools.contains(item))
                        return true;
                }
            }
            return false;
        }
    }

    private class BlockManager {
        public BlockManager() { }
        public BlockPos getNearestPlaceablePosition() {
            int range = 5;
            double closestLength = Double.POSITIVE_INFINITY;
            BlockPos closestBlockPos = null;
            Vec3d playerPos = MC.player.getPos();
            BlockState state = Registry.BLOCK.get(new Identifier("minecraft", "stone")).getDefaultState();
            for (int x = (int)playerPos.x - range; x <= (int)playerPos.x + range; x++) {
                for (int y = (int)playerPos.y - range; y <= (int)playerPos.y + range; y++) {
                    for (int z = (int)playerPos.z - range; z <= (int)playerPos.z + range; z++) {
                        if (playerPos.subtract(new Vec3d(x, y, z)).length() > range)
                            continue;
                        BlockPos currentPos = new BlockPos(x, y, z);
                        if (MC.player.getBoundingBox().intersects(state.getOutlineShape(MC.world, currentPos).getBoundingBox().offset(currentPos)))
                            continue;
                        BlockHitResult hitResult = new BlockHitResult(Vec3d.ZERO, Direction.UP, currentPos.down(), false);
                        if (MC.world.getBlockState(currentPos.down()).onUse(MC.world, MC.player, Hand.MAIN_HAND, hitResult).isAccepted())
                            continue;
                        if (MC.world.getBlockState(currentPos).isAir() &&
                                !MC.world.getBlockState(currentPos.down()).isAir() &&
                                BlockUtils.canBeClicked(currentPos.down())) {
                            double distance = new Vec3d(x, y, z).subtract(playerPos).lengthSquared();
                            if (distance < closestLength) {
                                closestLength = distance;
                                closestBlockPos = currentPos;
                            }
                        }
                    }
                }
            }
            return closestBlockPos;
        }
        public void placeBlock(Item block, BlockPos pos) {
            containerManager.openInventory();
            inventoryManager.equipItem(block, 0);
            inventoryManager.selectHotbarSlot(0);
            containerManager.closeScreen();
            IMC.getInteractionManager().rightClickBlock(pos.down(), Direction.UP, Vec3d.ZERO);
        }
    }

    private CraftingPlan getCraftingPlan(Identifier id) {
        List<CraftingProcess> processes = new ArrayList<>(processMap.getOrDefault(id, new ArrayList<>()));
        processes.add(new InventoryCraftingProcess(Registry.ITEM.get(id)));
        processes.add(new StorageCraftingProcess(Registry.ITEM.get(id)));
        return new CraftingPlan(Registry.ITEM.get(id), processes);
    }

    private class SlotUpdateInfo {
        public int slot;
        public ItemStack itemStack;
        public SlotUpdateInfo(int slot, ItemStack itemStack) {
            this.slot = slot;
            this.itemStack = itemStack;
        }
    }

    private abstract class StorageQuery<T> {
        public abstract HashMap<T, Integer> getAvailabilityMap();
        public abstract boolean acquire(T item, int count);
    }

    private class InventoryStorageQuery extends StorageQuery<Item> {
        public InventoryStorageQuery() { }
        @Override
        public HashMap<Item, Integer> getAvailabilityMap() {
            HashMap<Item, Integer> res = new HashMap<>();
            List<ItemStack> items = MC.player.getInventory().main;
            for (ItemStack cur : items) {
                if (cur.getCount() > 0)
                    res.put(cur.getItem(), res.getOrDefault(cur.getItem(), 0) + cur.getCount());
            }
            return res;
        }
        public HashMap<Item, Integer> getCurrentContainerAvailabilityMap() {
            HashMap<Item, Integer> res = new HashMap<>();
            List<Slot> slots = MC.player.currentScreenHandler.slots;
            for (int i = 0; i < slots.size() - 36; i++) {
                if (slots.get(i).getStack().getCount() > 0)
                    res.put(slots.get(i).getStack().getItem(), res.getOrDefault(slots.get(i).getStack().getItem(), 0) + slots.get(i).getStack().getCount());
            }
            return res;
        }
        @Override
        public boolean acquire(Item item, int count) { return true; }
    }

    private abstract class Pathfinder {
        private boolean supportsMining;
        public Pathfinder(boolean supportsMining) {
            this.supportsMining = supportsMining;
        }
        public boolean isMiningSupported() {
            return supportsMining;
        }
        public abstract boolean path(BlockPos pos);
        public abstract boolean mine(Block block, int count);
    }

    private class WurstPathfinder extends Pathfinder {
        public WurstPathfinder() {
            super(false);
        }
        public boolean path(BlockPos pos) {
            GoToCmd path = new GoToCmd();
            path.setGoal(pos);
            path.enable();
            path.waitUntilDone();
            return true;
        }
        public boolean mine(Block block, int count) {
            throw new NotImplementedException("Wurst pathfinder does not support mining");
        }
    }

    private abstract class NotifyingRunnable implements Runnable {
        protected boolean done = false;
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        public boolean isDone() {
            return done;
        }
        @Override
        public void run() {
            runInternal();
            lock.lock();
            try {
                done = true;
                condition.signalAll();
            }
            finally {
                lock.unlock();
            }
        }
        public void runUntilDone() {
            MC.execute(this);
            lock.lock();
            try {
                if (!isDone()) {
                    condition.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        public void runWithoutWaiting() {
            MC.execute(this);
        }
        protected abstract void runInternal();
    }

    private class BaritoneInterface {
        private Class BaritoneAPI;
        private Class GoalBlock;
        private Class Goal;
        public BaritoneInterface() {
            try {
                this.BaritoneAPI = Class.forName("baritone.api.BaritoneAPI");
                this.GoalBlock = Class.forName("baritone.api.pathing.goals.GoalBlock");
                this.Goal = Class.forName("baritone.api.pathing.goals.Goal");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        private void resetSetting(String setting) {
            try {
                Object res = getSetting(setting);
                res.getClass().getMethod("reset").invoke(res);
            }
            catch (Exception ex) { }
        }
        private Object getSetting(String setting) {
            try {
                Object settings = BaritoneAPI.getMethod("getSettings").invoke(BaritoneAPI);
                Object res = settings.getClass().getField(setting).get(settings);
                return res;
            }
            catch (Exception ex) {
                return null;
            }
        }
        private void setSetting(String setting, Object value) {
            try {
                Object res = getSetting(setting);
                res.getClass().getField("value").set(res, value);
            }
            catch (Exception ex) { }
        }
        private void setAllowInventory(boolean value) {
            setSetting("allowInventory", value);
        }
        private void setAcceptableThrowawayItems(List<Item> items) {
            setSetting("acceptableThrowawayItems", items);
        }
        private Object getPrimaryBaritone() {
            try {
                Object provider = BaritoneAPI.getMethod("getProvider").invoke(BaritoneAPI);
                Object baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
                return baritone;
            }
            catch (Exception ex) {
                return null;
            }
        }
        public Object getCustomGoalProcess() {
            try {
                Object baritone = getPrimaryBaritone();
                Object customGoalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
                return customGoalProcess;
            }
            catch (Exception ex) {
                return null;
            }
        }
        public Object getMineProcess() {
            try {
                Object baritone = getPrimaryBaritone();
                Object mineProcess = baritone.getClass().getMethod("getMineProcess").invoke(baritone);
                return mineProcess;
            }
            catch (Exception ex) {
                return null;
            }
        }
        public void setGoalAndPath(Object process, BlockPos pos) {
            try {
                Object goalBlock = GoalBlock.getConstructor(BlockPos.class).newInstance(pos);
                process.getClass().getMethod("setGoalAndPath", Goal).invoke(process, goalBlock);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        public void mine(Object process, int count, Block block) {
            try {
                process.getClass().getMethod("mine", int.class, Block[].class).invoke(process, new Object[] { count, new Block[] { block } });
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        public boolean isActive(Object process) {
            try {
                Object value = process.getClass().getMethod("isActive").invoke(process);
                return (Boolean) value;
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }
    }

    private class BaritonePathfinder extends Pathfinder {
        public BaritonePathfinder() {
            super(true);
        }
        public boolean path(BlockPos pos) {
            Object pathProcess = baritoneInterface.getCustomGoalProcess();
            baritoneInterface.setAllowInventory(true);
            baritoneInterface.setAcceptableThrowawayItems(new ArrayList<>());
            NotifyingRunnable baritoneRunnable = new NotifyingRunnable() {
                protected void runInternal() {
                    baritoneInterface.setGoalAndPath(pathProcess, pos);
                }
            };
            baritoneRunnable.runUntilDone();
            while (baritoneInterface.isActive(pathProcess)) {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            baritoneInterface.setAllowInventory(false);
            return true;
        }
        public boolean mine(Block block, int count) {
            updateTotalInventoryAvailability();
            Object mineProcess = baritoneInterface.getMineProcess();
            final int alreadyPossessed;
            if (isCurrentlyCrafting) {
                synchronized (totalInventoryAvailabilityMap) {
                    alreadyPossessed = totalInventoryAvailabilityMap.getOrDefault(block.asItem(), 0);
                }
            }
            else {
                alreadyPossessed = inventoryQuery.getAvailabilityMap().getOrDefault(block.asItem(), 0);
            }
            baritoneInterface.setAllowInventory(true);
            baritoneInterface.setAcceptableThrowawayItems(new ArrayList<>());
            NotifyingRunnable baritoneRunnable = new NotifyingRunnable() {
                protected void runInternal() {
                    baritoneInterface.mine(mineProcess, count + alreadyPossessed, block);
                }
            };
            baritoneRunnable.runUntilDone();
            while (baritoneInterface.isActive(mineProcess)) {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            baritoneInterface.setAllowInventory(false);
            return true;
        }
    }

    private class ContainerStorageQuery extends StorageQuery<Item> {
        private HashMap<BlockPos, HashMap<Item, Integer>> containers;
        public BlockPos nearestContainer = BlockPos.ORIGIN;
        public double nearestContainerDistance = 0.0;
        public ContainerStorageQuery() {
            containers = new HashMap<>();
        }
        public void calculateNearestContainer(Vec3d pos) {
            double minDistance = Double.POSITIVE_INFINITY;
            for (BlockPos container : containers.keySet()) {
                Vec3d containerPos = new Vec3d(container.getX(), container.getY(), container.getZ());
                double distance = pos.subtract(containerPos).length();
                if (distance < minDistance) {
                    nearestContainer = container;
                    minDistance = distance;
                }
            }
            nearestContainerDistance = minDistance;
        }
        public void updateContainer(HashMap<Item, Integer> content, BlockPos pos) {
            containers.put(pos, content);
        }
        @Override
        public HashMap<Item, Integer> getAvailabilityMap() {
            HashMap<Item, Integer> globalAvailabilityMap = new HashMap<>();
            for (HashMap<Item, Integer> map : containers.values()) {
                for (Item item : map.keySet()) {
                    globalAvailabilityMap.put(item, globalAvailabilityMap.getOrDefault(item, 0) + map.get(item));
                }
            }
            return globalAvailabilityMap;
        }
        private int takeItem(BlockPos containerPos, Item item, int count) {
            int initialCount = count;
            if (!containers.containsKey(containerPos))
                return count;
            HashMap<Item, Integer> container = containers.get(containerPos);
            ScreenHandler handler = MC.player.currentScreenHandler;
            for (int i = 0; i < handler.slots.size() - 36; i++) {
                if (count <= 0)
                    break;
                if (handler.getSlot(i).hasStack() && handler.getSlot(i).getStack().getItem().equals(item)) {
                    int amount = handler.getSlot(i).getStack().getCount();
                    if (amount > count) {
                        int emptySlotId = -1;
                        for (int x = handler.slots.size() - 36; x < handler.slots.size(); x++) {
                            if (!handler.getSlot(x).hasStack()) {
                                emptySlotId = x;
                                break;
                            }
                        }
                        if (emptySlotId != -1) {
                            MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                            for (int x = 0; x < count; x++)
                                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, emptySlotId, 1, SlotActionType.PICKUP, MC.player);
                            MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                            count = 0;
                        }
                    }
                    else {
                        count -= handler.getSlot(i).getStack().getCount();
                        synchronized (totalInventoryAvailabilityMap) {
                            totalInventoryAvailabilityMap.put(item, totalInventoryAvailabilityMap.getOrDefault(item, 0) + handler.getSlot(i).getStack().getCount());
                        }
                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, MC.player);
                    }
                }
            }
            container.put(item, container.getOrDefault(item, 0) - (initialCount - count));
            return count;
        }
        @Override
        public boolean acquire(Item item, int count) {
            for (BlockPos pos : containers.keySet()) {
                if (count <= 0)
                    break;
                if (containers.get(pos).getOrDefault(item, 0) > 0) {
                    containerManager.navigateAndOpenContainer(pos);
                    count = takeItem(pos, item, count);
                }
            }
            return count <= 0;
        }
        public List<BlockPos> getAcquireRoute(Item item, int count) {
            List<BlockPos> res = new ArrayList<>();
            for (BlockPos pos : containers.keySet()) {
                if (count <= 0)
                    break;
                if (containers.get(pos).getOrDefault(item, 0) > 0) {
                    res.add(pos);
                    count -= containers.get(pos).get(item);
                }
            }
            return res;
        }
        public void acquire(HashMap<Item, Integer> items) {
            for (BlockPos pos : containers.keySet()) {
                if (items.size() == 0)
                    break;
                HashMap<Item, Integer> container = containers.get(pos);
                Set<Item> itemKeySet = new HashSet<>(items.keySet());
                for (Item toAcquire : itemKeySet) {
                    if (container.getOrDefault(toAcquire, 0) > 0) {
                        containerManager.navigateAndOpenContainer(pos);
                        items.put(toAcquire, takeItem(pos, toAcquire, items.getOrDefault(toAcquire, 0)));
                        if (items.get(toAcquire) <= 0)
                            items.remove(toAcquire);
                    }
                }
            }
        }
    }

    private class WorldStorageQuery extends StorageQuery<Block> {
        public WorldStorageQuery() { }
        private List<Chunk> getChunks(int range) {
            List<Chunk> res = new ArrayList<>();
            ChunkPos playerPos = MC.player.getChunkPos();
            for (int x = playerPos.x - range; x <= playerPos.x + range; x++) {
                for (int z = playerPos.z - range; z <= playerPos.z + range; z++) {
                    Chunk chunk = MC.world.getChunk(x, z);
                    if (chunk instanceof EmptyChunk)
                        continue;
                    res.add(chunk);
                }
            }
            return res;
        }
        private void applyToBlocks(Consumer<Pair<BlockPos, BlockState>> func) {
            List<Chunk> chunks = getChunks(5);
            for (Chunk c : chunks) {
                ChunkPos chunkPos = c.getPos();
                int minX = chunkPos.getStartX();
                int minY = MC.world.getBottomY();
                int minZ = chunkPos.getStartZ();
                int maxX = chunkPos.getEndX();
                int maxY = MC.world.getTopY();
                int maxZ = chunkPos.getEndZ();
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            BlockState state = c.getBlockState(new BlockPos(x, y, z));
                            func.accept(new Pair<>(new BlockPos(x, y, z), state));
                        }
                    }
                }
            }
        }
        @Override
        public HashMap<Block, Integer> getAvailabilityMap() {
            HashMap<Block, Integer> availability = new HashMap<>();
            applyToBlocks((b) -> { availability.put(b.getRight().getBlock(), availability.getOrDefault(b.getRight().getBlock(), 0) + 1); });
            return availability;
        }
        public HashMap<Block, BlockPos> getNearestPositions() {
            HashMap<Block, BlockPos> res = new HashMap<>();
            Vec3d playerPos = MC.player.getPos();
            applyToBlocks((b) -> {
                Block block = b.getRight().getBlock();
                BlockPos pos = b.getLeft();
                if (!res.containsKey(block)) {
                    res.put(block, b.getLeft());
                    return;
                }
                BlockPos oldPos = res.get(block);
                double oldDistance = new Vec3d(oldPos.getX(), oldPos.getY(), oldPos.getZ()).subtract(playerPos).lengthSquared();
                if (new Vec3d(pos.getX(), pos.getY(), pos.getZ()).subtract(playerPos).lengthSquared() < oldDistance)
                    res.put(block, pos);
            });
            return res;
        }
        public HashMap<Block, Double> getNearestBlockDistances(HashMap<Block, BlockPos> nearestPositions) {
            HashMap<Block, Double> res = new HashMap<>();
            Vec3d playerPos = MC.player.getPos();
            for (Block block : nearestPositions.keySet()) {
                BlockPos pos = nearestPositions.get(block);
                Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                res.put(block, playerPos.subtract(blockPos).length());
            }
            return res;
        }
        public HashMap<Block, HashSet<BlockPos>> getLocations(HashSet<Block> blocks) {
            HashMap<Block, HashSet<BlockPos>> res = new HashMap<>();
            applyToBlocks((b) -> {
                if (!blocks.contains(b.getRight().getBlock()))
                    return;
                HashSet<BlockPos> s = res.getOrDefault(b.getRight().getBlock(), new HashSet<>());
                s.add(b.getLeft());
                res.put(b.getRight().getBlock(), s);
            });
            return res;
        }
        @Override
        public boolean acquire(Block block, int count) {
            return pathFinder.mine(block, count);
        }
    }

    private class CraftingPlan {
        private List<CraftingProcess> processes;
        private Item target;
        public CraftingPlan(Item target, List<CraftingProcess> processes) {
            this.processes = processes;
            this.target = target;
        }
        public Node getNode() {
            if (processes.size() == 0)
                return null;
            if (processes.size() > 1)
                return new ChoiceNode(target, 0, processes);
            return processes.get(0).getNode();
        }
    }

    private abstract class CraftingProcess {
        public abstract int getMultiplicity();
        public abstract Node getNode();
    }

    private class RecipeCraftingProcess extends CraftingProcess {
        private Recipe<?> recipe;
        public RecipeCraftingProcess(Recipe<?> recipe) {
            this.recipe = recipe;
        }
        @Override
        public int getMultiplicity() {
            int res = 1;
            for (Ingredient ing : recipe.getIngredients()) {
                res = Math.max(res, ing.getMatchingStacks().length);
            }
            return res;
        }
        @Override
        public Node getNode() {
            return new RecipeNode(recipe.getOutput().getItem(), 0, List.of(this));
        }
    }

    private class SmeltingCraftingProcess extends CraftingProcess {
        private Recipe<?> recipe;
        public SmeltingCraftingProcess(Recipe<?> recipe) {
            this.recipe = recipe;
        }
        @Override
        public int getMultiplicity() {
            int res = 1;
            for (Ingredient ing : recipe.getIngredients()) {
                res = Math.max(res, ing.getMatchingStacks().length);
            }
            return res;
        }
        @Override
        public Node getNode() {
            return new SmeltingNode(recipe.getOutput().getItem(), 0, List.of(this));
        }
    }

    private class InventoryCraftingProcess extends CraftingProcess {
        private Item item;
        public InventoryCraftingProcess(Item item) {
            this.item = item;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new InventoryNode(item, 0, List.of(this));
        }
    }

    private class StorageCraftingProcess extends CraftingProcess {
        private Item item;
        public StorageCraftingProcess(Item item) {
            this.item = item;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new StorageNode(item, 0, List.of(this));
        }
    }

    private class WorldCraftingProcess extends CraftingProcess {
        private Block block;
        private Item dropped;
        public WorldCraftingProcess(Item dropped, Block block) {
            this.block = block;
            this.dropped = dropped;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new WorldNode(dropped, block, 0, List.of(this));
        }
    }

    private class PathingCraftingProcess extends CraftingProcess {
        private Block block;
        public PathingCraftingProcess(Block block) {
            this.block = block;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new PathingNode(block.asItem(), 0, List.of(this));
        }
    }

    private class PlacementCraftingProcess extends CraftingProcess {
        private Block block;
        public PlacementCraftingProcess(Block block) {
            this.block = block;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new PlacementNode(block.asItem(), 0, List.of(this));
        }
    }

    private class WorkbenchCraftingProcess extends CraftingProcess {
        private Block block;
        public WorkbenchCraftingProcess(Block block) {
            this.block = block;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new WorkbenchNode(block.asItem(), 0, List.of(this));
        }
    }

    private class ToolCraftingProcess extends CraftingProcess {
        private Block block;
        public ToolCraftingProcess(Block block) {
            this.block = block;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new ToolNode(block.asItem(), 0, List.of(this));
        }
    }

    private class ContainerManager {
        private HashMap<Block, HashSet<BlockPos>> containers = new HashMap<>();
        private BlockPos currentContainer = null;
        private BlockPos getNearestPathablePosition(BlockPos containerPos) {
            int range = 5;
            double closestLength = Double.POSITIVE_INFINITY;
            BlockPos closestBlockPos = null;
            Vec3d playerPos = MC.player.getPos();
            Vec3d container = new Vec3d(containerPos.getX(), containerPos.getY(), containerPos.getZ());
            for (int x = (int)container.x - range; x <= (int)container.x + range; x++) {
                for (int y = (int)container.y - range; y <= (int)container.y + range; y++) {
                    for (int z = (int)container.z - range; z <= (int)container.z + range; z++) {
                        if (container.subtract(new Vec3d(x, y, z)).length() > range)
                            continue;
                        BlockPos currentPos = new BlockPos(x, y, z);
                        if (MC.world.getBlockState(currentPos).isAir() &&
                            MC.world.getBlockState(currentPos.up()).isAir() &&
                            BlockUtils.canBeClicked(currentPos.down())) {
                            double distance = new Vec3d(x, y, z).subtract(playerPos).lengthSquared();
                            if (distance < closestLength) {
                                closestLength = distance;
                                closestBlockPos = currentPos;
                            }
                        }
                    }
                }
            }
            return closestBlockPos;
        }
        public void openInventory() {
            if (MC.currentScreen instanceof InventoryScreen)
                return;
            if (MC.currentScreen != null)
                closeScreen();
            NotifyingRunnable inventoryRunnable = new NotifyingRunnable() {
                @Override
                protected void runInternal() {
                    MC.getTutorialManager().onInventoryOpened();
                    InventoryScreen screen = new InventoryScreen(MC.player);
                    screen.refreshRecipeBook();
                    MC.setScreen(screen);
                }
            };
            inventoryRunnable.runUntilDone();
        }
        public boolean navigateToContainer(BlockPos container) {
            if (container.equals(currentContainer))
                return true;
            if (MC.currentScreen != null)
                closeScreen();
            BlockPos nearestPathablePosition = getNearestPathablePosition(container);
            if (nearestPathablePosition == null)
                nearestPathablePosition = container.up();
            return pathFinder.path(nearestPathablePosition);
        }
        public boolean navigateAndOpenContainer(BlockPos container) {
            if (!navigateToContainer(container))
                return false;
            IMC.getInteractionManager().rightClickBlock(container, Direction.NORTH, Vec3d.ZERO);
            awaitContainerOpen();
            currentContainer = container;
            return true;
        }
        private void closeScreen() {
            currentContainer = null;
            try {
                MC.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(MC.player.currentScreenHandler.syncId));
                MC.getNetworkHandler().onCloseScreen(new CloseScreenS2CPacket(MC.player.currentScreenHandler.syncId));
            }
            catch (OffThreadException e) {
                e.printStackTrace();
            }
        }
        public void addContainer(Block block, BlockPos pos) {
            HashSet<BlockPos> positions = containers.getOrDefault(block, new HashSet<>());
            positions.add(pos);
            containers.put(block, positions);
        }
        public void updateContainers(HashMap<Block, HashSet<BlockPos>> updated) {
            for (Block b : updated.keySet()) {
                HashSet<BlockPos> positions = containers.getOrDefault(b, new HashSet<>());
                positions.addAll(updated.get(b));
                containers.put(b, positions);
            }
        }
        public BlockPos getClosestToPlayer(Block b) {
            if (!containers.containsKey(b))
                return null;
            double shortestDistance = Double.POSITIVE_INFINITY;
            BlockPos nearestPos = null;
            Vec3d playerPos = MC.player.getPos();
            for (BlockPos pos : containers.get(b)) {
                Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                double distance = playerPos.subtract(blockPos).lengthSquared();
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    nearestPos = pos;
                }
            }
            return nearestPos;
        }
        public HashMap<Block, HashSet<BlockPos>> getContainers() {
            return containers;
        }
    }

    private class InventoryManager {
        public InventoryManager() { }
        public int getHotbarSlot(int i) {
            return getStartingSlot() + 27 + i;
        }
        public int getStartingSlot() {
            return MC.player.currentScreenHandler.slots.size() - 37;
        }
        public void leftClickSlot(int slot) {
            MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, MC.player);
        }
        public void swapSlots(int slot1, int slot2) {
            System.out.println("Slot 1: " + slot1);
            System.out.println("Slot 2: " + slot2);
            leftClickSlot(slot1);
            leftClickSlot(slot2);
            leftClickSlot(slot1);
        }
        public void equipItem(Item item, int hotbarSlot) {
            for (int i = getStartingSlot(); i < getStartingSlot() + 36; i++) {
                if (!MC.player.currentScreenHandler.slots.get(i).hasStack())
                    continue;
                if (MC.player.currentScreenHandler.slots.get(i).getStack().getItem().equals(item)) {
                    swapSlots(i, getHotbarSlot(hotbarSlot));
                    break;
                }
            }
        }
        public void selectHotbarSlot(int slot) {
            MC.player.getInventory().selectedSlot = slot;
        }
    }

    private class CraftingState {
        private HashMap<Item, Integer> inventoryAvailability;
        private HashMap<Item, Integer> storageAvailability;
        private HashMap<Block, Integer> worldAvailability;
        private HashMap<Integer, Item> toolAvailability;
        private HashMap<Integer, Double> efficiencyMap = new HashMap<>();
        private HashMap<Integer, Double> naiveEfficiencyMap = new HashMap<>();
        private HashSet<Node> visited;
        private List<StorageNode> storageNodes = new ArrayList<>();
        private HashMap<Item, Integer> craftingItemFrequency = new HashMap<>();
        private HashSet<Integer> deadNodes = new HashSet<>();
        private boolean success = false;
        public CraftingState(HashMap<Item, Integer> inventoryAvailability, HashMap<Item, Integer> storageAvailability, HashMap<Block, Integer> worldAvailability, HashSet<Node> visited, HashMap<Integer, Item> toolAvailability) {
            this.inventoryAvailability = inventoryAvailability;
            this.storageAvailability = storageAvailability;
            this.worldAvailability = worldAvailability;
            this.toolAvailability = toolAvailability;
            this.visited = visited;
        }
        public CraftingState setSuccess(boolean value) {
            success = value;
            return this;
        }
        public CraftingState success() {
            success = true;
            return this;
        }
        public CraftingState failure() {
            success = false;
            return this;
        }
        public CraftingState clone() {
            CraftingState state = new CraftingState((HashMap<Item, Integer>) inventoryAvailability.clone(), (HashMap<Item, Integer>) storageAvailability.clone(), (HashMap<Block, Integer>) worldAvailability.clone(), (HashSet<Node>) visited.clone(), (HashMap<Integer, Item>) toolAvailability.clone());
            state.success = success;
            state.storageNodes = new ArrayList<>(storageNodes);
            state.craftingItemFrequency = (HashMap<Item, Integer>) craftingItemFrequency.clone();
            state.efficiencyMap = (HashMap<Integer, Double>) efficiencyMap.clone();
            state.naiveEfficiencyMap = naiveEfficiencyMap;
            state.deadNodes = (HashSet<Integer>) deadNodes.clone();
            return state;
        }
        public void set(CraftingState other) {
            inventoryAvailability.clear();
            inventoryAvailability.putAll(other.inventoryAvailability);
            storageAvailability.clear();
            storageAvailability.putAll(other.storageAvailability);
            worldAvailability.clear();
            worldAvailability.putAll(other.worldAvailability);
            toolAvailability.clear();
            toolAvailability.putAll(other.toolAvailability);
            visited.clear();
            visited.addAll(other.visited);
            success = other.success;
            storageNodes.clear();
            storageNodes.addAll(other.storageNodes);
            craftingItemFrequency.clear();
            craftingItemFrequency.putAll(other.craftingItemFrequency);
            efficiencyMap.clear();
            efficiencyMap.putAll(other.efficiencyMap);
            naiveEfficiencyMap.clear();
            naiveEfficiencyMap.putAll(other.efficiencyMap);
            deadNodes.clear();
            deadNodes.addAll(other.deadNodes);
        }
    }

    private boolean awaitSlotUpdate(Item item, int amount, int slot, boolean onlyConsiderItem, boolean succeedAfterTimeout) {
        slotUpdateLock.lock();
        try {
            while (latestSlotUpdate == null || !latestSlotUpdate.itemStack.getItem().equals(item) || (!onlyConsiderItem && (latestSlotUpdate.itemStack.getCount() != amount || latestSlotUpdate.slot != slot))) {
                boolean gotSignal = slotUpdateCondition.await(1000, TimeUnit.MILLISECONDS);
                if (succeedAfterTimeout)
                    return true;
                if (!gotSignal) {
                    ItemStack craftingItem = MC.player.currentScreenHandler.getSlot(0).getStack();
                    if (craftingItem.getItem().equals(item) && (onlyConsiderItem || (craftingItem.getCount() == amount && slot == 0)))
                        return true;
                    return false;
                }
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            slotUpdateLock.unlock();
        }
        return true;
    }

    private class VerificationInfo {
        public int amountToCraft;
        public boolean success;
        double timeHeuristic;
        public VerificationInfo(int amountToCraft, boolean success, double timeHeuristic) {
            this.amountToCraft = amountToCraft;
            this.success = success;
            this.timeHeuristic = timeHeuristic;
        }
        public VerificationInfo() {
            amountToCraft = 0;
            success = false;
            timeHeuristic = 0;
        }
    }

    private static class CraftingParams {
        public boolean generateTree;
        public boolean useTree;
        public boolean collectStorageNodes;
        public boolean execute;
        public boolean createVerificationInfo;
        public static final CraftingParams VERIFY_AND_GENERATE = new CraftingParams().setGenerateTree(true).setCollectStorageNodes(true).setCreateVerificationInfo(true);
        public static final CraftingParams CRAFT = new CraftingParams().setExecute(true).setUseTree(true);
        public CraftingParams() { }
        public CraftingParams setGenerateTree(boolean generateTree) {
            this.generateTree = generateTree;
            return this;
        }
        public CraftingParams setCollectStorageNodes(boolean collectStorageNodes) {
            this.collectStorageNodes = collectStorageNodes;
            return this;
        }
        public CraftingParams setExecute(boolean execute) {
            this.execute = execute;
            return this;
        }
        public CraftingParams setCreateVerificationInfo(boolean createVerificationInfo) {
            this.createVerificationInfo = createVerificationInfo;
            return this;
        }
        public CraftingParams setUseTree(boolean useTree) {
            this.useTree = useTree;
            return this;
        }
    }

    private abstract class Node {
        protected Item target;
        protected int needed;
        protected List<CraftingProcess> processes;
        protected int stackShift;
        protected List<Node> children;
        protected int naiveMaxCraftable;
        protected boolean generatedNaiveMaxCraftable = false;
        protected int maxCraftable;
        protected int nodeId;
        protected int timeTaken;
        protected int callCounter;
        private static int totalNumCalls = 0;
        private static int maxNodeId = 0;
        public Node(Item target, int needed, List<CraftingProcess> processes) {
            nodeId = maxNodeId++;
            children = new ArrayList<>();
            this.processes = processes;
            this.target = target;
            this.needed = needed;
            stackShift = 0;
            timeTaken = 0;
            callCounter = 0;
        }
        protected boolean requiresAllChildren() {
            return true;
        }
        protected void mergeResources(Resources<OperableInteger> base, Resources<OperableInteger> res) {
            for (Integer item : res.keySet()) {
                if (base.containsKey(item)) {
                    Pair<OperableInteger, ResourceDomain> baseItem = base.get(item);
                    baseItem.setLeft(baseItem.getLeft().add(res.get(item).getLeft()));
                }
                else {
                    base.put(item, res.get(item));
                }
            }
        }
        public List<Pair<ItemStack, ResourceDomain>> toItemStack(Pair<Boolean, Resources<OperableInteger>> res) {
            List<Pair<ItemStack, ResourceDomain>> result = new ArrayList<>();
            if (res.getRight().containsKey(nodeId)) {
                result.add(new Pair<>(new ItemStack(target, res.getRight().get(nodeId).getLeft().getValue()), res.getRight().get(nodeId).getRight()));
            }
            for (Node child : children) {
                result.addAll(child.toItemStack(res));
            }
            return result;
        }
        public Node setNeeded(int needed) {
            this.needed = needed;
            return this;
        }
        public Node setStackShift(int stackShift) {
            this.stackShift = stackShift;
            return this;
        }
        private boolean generateTree(HashSet<Node> visited, CraftingState state) {
            if (!canPossiblyCraft(state))
                return false;
            if (shouldRememberVisit())
                visited.add(this);
            children = getChildren(visited, state);
            int initialChildren = children.size();
            for (int i = children.size() - 1; i >= 0; i--) {
                Node child = children.get(i);
                child.generateTree(visited, state);
                child.genNaiveMaxCraftable(state);
                if (child.naiveMaxCraftable == 0) {
                    if (requiresAllChildren()) {
                        if (shouldRememberVisit())
                            visited.remove(this);
                        return false;
                    }
                    else {
                        children.remove(i);
                    }
                }
            }
            if (shouldRememberVisit())
                visited.remove(this);
            if (initialChildren > 0 && children.size() == 0)
                return false;
            return true;
        }
        private int reorderNodeIds(int id) {
            nodeId = id;
            int next = id + 1;
            for (Node child : children) {
                next = child.reorderNodeIds(next);
            }
            return next;
        }
        public boolean doCraft() {
            if (needed == 0)
                return true;
            for (Node child : children) {
                if (!child.doCraft()) {
                    return false;
                }
            }
            return execute();
        }
        public void craft(int amount) {
            calculateMaxCraftable(amount);
            doCraft();
            /*while (!doCraft())
                calculateMaxCraftable(amount);*/
        }
        protected abstract List<Node> getChildrenInternal(HashSet<Node> nodes);
        private List<Node> getChildren(HashSet<Node> nodes, CraftingState state) {
            List<Node> res = new ArrayList<>();
            if (processes == null)
                return res;
            for (Node child : getChildrenInternal(nodes)) {
                if (child.shouldRememberVisit() && nodes.contains(child))
                    continue;
                res.add(child);
            }
            return res;
        }
        protected boolean canPossiblyCraft(CraftingState state) {
            return true;
        }
        protected abstract void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state);
        private CraftingState createFreshState() {
            HashMap<Item, Integer> inventoryAvailability = (HashMap<Item, Integer>) inventoryAvailabilityMap.clone();
            HashMap<Item, Integer> storageAvailability = (HashMap<Item, Integer>) storageAvailabilityMap.clone();
            HashMap<Block, Integer> worldAvailability = (HashMap<Block, Integer>) worldAvailabilityMap.clone();
            CraftingState state = new CraftingState(inventoryAvailability, storageAvailability, worldAvailability, new HashSet<>(), new HashMap<>());
            return state;
        }
        protected HashMap<Item, HashMap<Integer, Integer>> deepcopyExcess(HashMap<Item, HashMap<Integer, Integer>> excess) {
            HashMap<Item, HashMap<Integer, Integer>> res = new HashMap<>();
            for (Item item : excess.keySet()) {
                res.put(item, (HashMap<Integer, Integer>) excess.get(item).clone());
            }
            return res;
        }
        protected abstract void calculateLowerEfficiencyBound(CraftingState state);
        protected void genNaiveEfficiencyMap(CraftingState state) {
            if (naiveMaxCraftable == 0)
                return;
            for (Node child : children) {
                child.genNaiveEfficiencyMap(state);
            }
            calculateLowerEfficiencyBound(state);
        }
        private int calculateMaxCraftableInternal(int upperBound, boolean useHeuristic, boolean setMaxCraftable, boolean populateNaiveMetrics) {
            CraftingState state = createFreshState();
            if (populateNaiveMetrics) {
                containerQuery.calculateNearestContainer(MC.player.getPos());
                clearNaiveMaxCraftable();
                genNaiveMaxCraftable(state.clone());
                genNaiveEfficiencyMap(state);
            }
            HashMap<Integer, Integer> neededMap = new HashMap<>();
            CraftingState newState = state.clone();
            HashMap<Item, HashMap<Integer, Integer>> excess = new HashMap<>();
            Pair<Boolean, Resources<OperableInteger>> resources = getBaseResources(1, 1, deepcopyExcess(excess), (HashMap<Integer, Integer>) neededMap.clone(), newState.clone(), useHeuristic);
            int amount = 0;
            while (amount < upperBound && resources.getLeft()) {
                amount++;
                state = newState;
                newState = state.clone();
                if (!consumeResources(resources.getRight(), newState, excess, neededMap, 0)) {
                    newState = state.clone();
                    resources = getBaseResources(1, 1, deepcopyExcess(excess), (HashMap<Integer, Integer>) neededMap.clone(), newState.clone(), useHeuristic);
                }
            }
            applyNeededMap(neededMap);
            if (setMaxCraftable)
                applyMaxCraftableMap(neededMap);
            return amount;
        }
        private int getNumNodes() {
            int res = 1;
            for (Node child : children) {
                res += child.getNumNodes();
            }
            return res;
        }
        private void exportFile() {
            try {
                FileOutputStream fos = new FileOutputStream("C:\\Users\\laaks\\Desktop\\crafting.txt");
                PrintStream stream = new PrintStream(fos);
                print(0, true, stream);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        private int print(int indent, boolean ignoreNeeded, PrintStream stream) {
            if (ignoreNeeded || needed > 0) {
                String indentation = "";
                for (int i = 0; i < indent; i++) {
                    indentation += " ";
                }
                stream.println(indentation + target + ": " + needed + ", time " + timeTaken + ", calls " + callCounter + ", " + this.getClass());
                int calls = 1;
                for (Node child : children) {
                    calls += child.print(indent + 1, ignoreNeeded, stream);
                }
                return calls;
            }
            return 0;
        }
        private int calculateMaxCraftable(int upperBound) {
            globalTimeTaken = BigInteger.ZERO;
            generateTree(new HashSet<>(), createFreshState());
            reorderNodeIds(0);
            int amount = calculateMaxCraftableInternal(upperBound, true, true, true);
            int numPrinted = print(0, false, System.out);
            //exportFile();
            System.out.println("Total number of nodes: " + maxNodeId);
            System.out.println("Number of nodes printed: " + numPrinted);
            System.out.println("Craftable: " + amount);
            System.out.println("Nodes in tree: " + getNumNodes());
            System.out.println("Total number of calls: " + totalNumCalls);
            BigInteger[] arr = globalTimeTaken.divideAndRemainder(new BigInteger("1000000000"));
            System.out.println("Time taken: " + arr[0] + ", " + arr[1]);
            return amount;
        }
        private void applyNeededMap(HashMap<Integer, Integer> neededMap) {
            needed = neededMap.getOrDefault(nodeId, 0);
            for (Node child : children) {
                child.applyNeededMap(neededMap);
            }
        }
        private void applyMaxCraftableMap(HashMap<Integer, Integer> maxCraftableMap) {
            maxCraftable = maxCraftableMap.getOrDefault(nodeId, 0);
            for (Node child : children) {
                child.applyMaxCraftableMap(maxCraftableMap);
            }
        }
        protected boolean consumeResources(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            if (!state.deadNodes.contains(nodeId) && resources.containsKey(nodeId) && resources.get(nodeId).getLeft().getValue() > 0) {
                Pair<OperableInteger, ResourceDomain> resource = resources.get(nodeId);
                ResourceDomain domain = resource.getRight();
                int numNeeded = resource.getLeft().getValue();
                if (excess.containsKey(target)) {
                    HashMap<Integer, Integer> itemMap = excess.get(target);
                    for (int id : itemMap.keySet()) {
                        if (id <= nodeId) {
                            int reductionFactor = Math.min(numNeeded, itemMap.get(id));
                            numNeeded -= reductionFactor;
                            excessOverflow += reductionFactor;
                            itemMap.put(id, itemMap.get(id) - reductionFactor);
                        }
                        if (numNeeded <= 0)
                            break;
                    }
                }
                int originalNeeded = neededMap.getOrDefault(nodeId, 0);
                int outputCount = getOutputCount();
                if (outputCount > 1) {
                    int neededToCraft = getNeededToCraft(originalNeeded + numNeeded);
                    int oldNeededToCraft = getNeededToCraft(originalNeeded);
                    int leftover = neededToCraft - (originalNeeded + numNeeded);
                    int oldLeftover = oldNeededToCraft - originalNeeded;
                    int leftoverDifference = leftover - oldLeftover;
                    excess.put(target, excess.getOrDefault(target, new HashMap<>()));
                    int excessAmount = excess.get(target).getOrDefault(nodeId, 0) + leftoverDifference;
                    int shift = (int) Math.floor((double) excessAmount / outputCount) * outputCount;
                    excess.get(target).put(nodeId, excessAmount - shift);
                    numNeeded -= shift;
                }
                neededMap.put(nodeId, originalNeeded + numNeeded);
                if (domain == ResourceDomain.INVENTORY) {
                    if (state.inventoryAvailability.getOrDefault(target, 0) < resource.getLeft().getValue())
                        return false;
                    state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) - resource.getLeft().getValue());
                } else if (domain == ResourceDomain.STORAGE) {
                    if (state.storageAvailability.getOrDefault(target, 0) < resource.getLeft().getValue())
                        return false;
                    state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) - resource.getLeft().getValue());
                } else if (domain == ResourceDomain.WORLD) {
                    if (state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) < resource.getLeft().getValue())
                        return false;
                    state.worldAvailability.put(((WorldCraftingProcess) processes.get(0)).block, state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) - resource.getLeft().getValue());
                }
                return consumeResourcesInternal(resources, state, excess, neededMap, excessOverflow);
            }
            return true;
        }
        protected boolean consumeResourcesInternal(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            for (Node child : children) {
                if (!child.consumeResources(resources, state, excess, neededMap, excessOverflow))
                    return false;
            }
            return true;
        }
        protected Resources<OperableInteger> stackResources(int factor, Resources<OperableInteger> resources) {
            Resources<OperableInteger> res = new Resources<>();
            int nodeValue = resources.getOrDefault(nodeId, new Pair<>(new OperableInteger(1), ResourceDomain.COMPOSITE)).getLeft().getValue();
            stackResourcesInternal(factor * nodeValue, res, resources);
            return res;
        }
        protected abstract void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src);
        private Pair<Boolean, Resources<OperableInteger>> getBaseResources(int numNeeded, int actualNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            long startTime = System.currentTimeMillis();
            Pair<Boolean, Resources<OperableInteger>> result = new Pair<>(true, new Resources<>());
            if (excess.containsKey(target)) {
                HashMap<Integer, Integer> targetExcess = excess.get(target);
                for (int node : targetExcess.keySet()) {
                    if (actualNeeded == 0)
                        break;
                    if (node <= nodeId) {
                        int nodeValue = targetExcess.get(node);
                        int reductionFactor = Math.min(actualNeeded, nodeValue);
                        actualNeeded -= reductionFactor;
                        targetExcess.put(node, nodeValue - reductionFactor);
                    }
                }
            }
            if (numNeeded > 0)
                result = getBaseResourcesInternal(numNeeded, actualNeeded, neededMap, state, useHeuristic, excess);
            calculateExecutionTime(result.getRight(), state);
            long endTime = System.currentTimeMillis();
            timeTaken += (int)(endTime - startTime);
            callCounter++;
            totalNumCalls++;
            return result;
        }
        protected abstract Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess);
        protected abstract void genNaiveMaxCraftableInternal(CraftingState state);
        private void clearNaiveMaxCraftable() {
            for (Node child : children) {
                child.clearNaiveMaxCraftable();
            }
            naiveMaxCraftable = 0;
            generatedNaiveMaxCraftable = false;
        }
        protected void genNaiveMaxCraftable(CraftingState state) {
            if (generatedNaiveMaxCraftable)
                return;
            for (Node child : children) {
                child.genNaiveMaxCraftable(state);
            }
            genNaiveMaxCraftableInternal(state);
            generatedNaiveMaxCraftable = true;
        }
        protected abstract boolean shouldRememberVisit();
        public abstract HashMap<Item, ItemStack> collectIngredients();
        public abstract boolean execute();
        public abstract int getOutputCount();
        public int getNeededToCraft(int amount) {
            return (int)Math.ceil((double)amount / getOutputCount()) * getOutputCount();
        }
        @Override
        public int hashCode() {
            return target.hashCode();
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof Node) {
                Node o = (Node)other;
                return target.equals(o.target);
            }
            return false;
        }
    }

    private class SmeltingNode extends Node {
        private Node fuel;
        private Node furnace;
        public SmeltingNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    if (child == furnace) {
                        child.stackResourcesInternal(1, dest, src);
                    }
                    else if (child == fuel) {
                        child.stackResourcesInternal((num / 8) + (num % 8 > 0 ? 1 : 0), dest, src);
                    }
                    else {
                        child.stackResourcesInternal(num, dest, src);
                    }
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            List<Node> res = new ArrayList<>();
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            for (ItemStack stack : ingredients.values()) {
                Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
                CraftingPlan plan = getCraftingPlan(itemIdentifier);
                Node child = plan.getNode();
                res.add(child);
            }
            Block furnaceBlock = Registry.BLOCK.get(new Identifier("minecraft", "furnace"));
            furnace = new WorkbenchCraftingProcess(furnaceBlock).getNode();
            res.add(furnace);
            Identifier coalIdentifier = new Identifier("minecraft", "coal");
            fuel = getCraftingPlan(coalIdentifier).getNode();
            res.add(fuel);
            return res;
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.0;
            for (Node child : children) {
                executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            if (result.containsKey(nodeId))
                executionTime += result.get(nodeId).getLeft().getValue() * 1000;
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.05;
            for (Node child : children) {
                executionTime += state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected boolean consumeResourcesInternal(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            for (Node child : children) {
                if (!child.consumeResources(resources, state, excess, neededMap, 0))
                    return false;
            }
            return true;
        }
        private int getChildNeededFactor(Node child, int num) {
            if (child == fuel)
                return (num / 8) + (num % 8 > 0 ? 1 : 0);
            return num;
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            for (Node child : children) {
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(getChildNeededFactor(child, numNeeded), getChildNeededFactor(child, actualNeeded), excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, res);
                mergeResources(res, childRes.getRight());
            }
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            if (children.size() == 0) {
                naiveMaxCraftable = 0;
                return;
            }
            int outputFactor = Integer.MAX_VALUE;
            for (Node child : children) {
                outputFactor = Math.min(outputFactor, child.naiveMaxCraftable * (child == fuel ? 8 : 1));
            }
            naiveMaxCraftable = outputFactor;
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            HashMap<Item, ItemStack> stackTypes = new HashMap<>();
            for (Ingredient ing : ((SmeltingCraftingProcess)processes.get(0)).recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                if (stackTypes.containsKey(stack.getItem())) {
                    ItemStack value = stackTypes.get(stack.getItem());
                    value.setCount(value.getCount() + stack.getCount());
                }
                else {
                    stackTypes.put(stack.getItem(), stack.copy());
                }
            }
            return stackTypes;
        }
        @Override
        public int getOutputCount() {
            return ((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
        }
        private int calculateCraftingOutput() {
            List<Ingredient> ingredients = ((SmeltingCraftingProcess)processes.get(0)).recipe.getIngredients();
            HashMap<Item, ItemStack> collected = collectIngredients();
            int output = Integer.MAX_VALUE;
            for (Ingredient ing : ingredients) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack itemStack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                synchronized (totalInventoryAvailabilityMap) {
                    int outputFactor = Math.min(totalInventoryAvailabilityMap.getOrDefault(itemStack.getItem(), 0) / collected.get(itemStack.getItem()).getCount(), itemStack.getItem().getMaxCount()) * ((SmeltingCraftingProcess) processes.get(0)).recipe.getOutput().getCount();
                    output = Math.min(output, outputFactor);
                }
            }
            return output;
        }
        private void adjustTotalAvailability(Recipe<?> recipe, int craftingOutput) {
            Item outputItem = recipe.getOutput().getItem();
            synchronized (totalInventoryAvailabilityMap) {
                totalInventoryAvailabilityMap.put(outputItem, totalInventoryAvailabilityMap.getOrDefault(outputItem, 0) + craftingOutput);
            }
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                synchronized (totalInventoryAvailabilityMap) {
                    totalInventoryAvailabilityMap.put(stack.getItem(), totalInventoryAvailabilityMap.getOrDefault(stack.getItem(), 0) - (stack.getCount() * craftingOutput) / recipe.getOutput().getCount());
                }
            }
        }
        private void arrangeRecipe(Recipe<?> recipe, int craftAmount) {
            int craftingGridSize = 1;
            int width = 1;
            int slotNumber = 0;
            for (Ingredient ing : recipe.getIngredients()) {
                int currentCraftAmount = craftAmount;
                if (ing.getMatchingStacks().length > 0) {
                    int slotX = slotNumber % width;
                    int slotY = slotNumber / width;
                    int slot = slotY * craftingGridSize + slotX + 1;
                    List<Slot> slots = MC.player.currentScreenHandler.slots;
                    for (int i = slots.size() - 37; i < slots.size(); i++) {
                        if (currentCraftAmount <= 0)
                            break;
                        if (slots.get(i).getStack().getItem().equals(ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length].getItem())) {
                            int slotAmount = slots.get(i).getStack().getCount();
                            MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                            if (slotAmount <= currentCraftAmount) {
                                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, MC.player);
                                currentCraftAmount -= slotAmount;
                            } else {
                                for (int j = 0; j < currentCraftAmount; j++) {
                                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 1, SlotActionType.PICKUP, MC.player);
                                }
                                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                                currentCraftAmount = 0;
                            }
                        }
                    }
                }
                slotNumber++;
            }
            int coalCraftAmount = craftAmount / 8 + (craftAmount % 8 > 0 ? 1 : 0);
            Item coalItem = Registry.ITEM.get(new Identifier("minecraft", "coal"));
            int fuelSlot = 2;
            List<Slot> slots = MC.player.currentScreenHandler.slots;
            for (int i = slots.size() - 37; i < slots.size(); i++) {
                if (coalCraftAmount <= 0)
                    break;
                if (slots.get(i).getStack().getItem().equals(coalItem)) {
                    int slotAmount = slots.get(i).getStack().getCount();
                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                    if (slotAmount <= coalCraftAmount) {
                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, fuelSlot, 0, SlotActionType.PICKUP, MC.player);
                        coalCraftAmount -= slotAmount;
                    } else {
                        for (int j = 0; j < coalCraftAmount; j++) {
                            MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, fuelSlot, 1, SlotActionType.PICKUP, MC.player);
                        }
                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                        coalCraftAmount = 0;
                    }
                }
            }
        }
        @Override
        public boolean execute() {
            if (!usingFurnace()) {
                Block furnaceBlock = Registry.BLOCK.get(new Identifier("minecraft", "furnace"));
                BlockPos nearestFurnace = containerManager.getClosestToPlayer(furnaceBlock);
                if (nearestFurnace != null)
                    containerManager.navigateAndOpenContainer(nearestFurnace);
            }
            int neededToCraft = needed;
            int craftingOutput = 0;
            while ((craftingOutput = calculateCraftingOutput()) <= neededToCraft && craftingOutput > 0) {
                if (!usingFurnace()) return false;
                arrangeRecipe(((SmeltingCraftingProcess) processes.get(0)).recipe, craftingOutput / ((SmeltingCraftingProcess) processes.get(0)).recipe.getOutput().getCount());
                if (!awaitSlotUpdate(((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false))
                    return false;
                if (!usingFurnace()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false);
                adjustTotalAvailability(((SmeltingCraftingProcess)processes.get(0)).recipe, craftingOutput);
                neededToCraft -= craftingOutput;
            }
            for (int i = 0; i < neededToCraft / ((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getCount(); i++) {
                if (!usingFurnace()) return false;
                arrangeRecipe(((SmeltingCraftingProcess) processes.get(0)).recipe, 1);
                if (!awaitSlotUpdate(((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false))
                    return false;
                if (!usingFurnace()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false);
                adjustTotalAvailability(((SmeltingCraftingProcess)processes.get(0)).recipe, ((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getCount());
            }
            return true;
        }
    }

    private class RecipeNode extends Node {
        public RecipeNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            int neededToCraft = getNeededToCraft(num);
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    if (ingredients.containsKey(child.target)) {
                        child.stackResourcesInternal((neededToCraft * ingredients.get(child.target).getCount()) / process.recipe.getOutput().getCount(), dest, src);
                    }
                    else {
                        child.stackResourcesInternal(1, dest, src);
                    }
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            List<Node> res = new ArrayList<>();
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            for (ItemStack stack : ingredients.values()) {
                Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
                CraftingPlan plan = getCraftingPlan(itemIdentifier);
                Node child = plan.getNode();
                res.add(child);
            }
            if (!((RecipeCraftingProcess) processes.get(0)).recipe.fits(2, 2)) {
                Block craftingTable = Registry.BLOCK.get(new Identifier("minecraft", "crafting_table"));
                res.add(new WorkbenchCraftingProcess(craftingTable).getNode());
            }
            return res;
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.05;
            for (Node child : children) {
                executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.05;
            for (Node child : children) {
                executionTime += state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected boolean consumeResourcesInternal(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            int toConsume = 0;
            int amount = 0;
            if (resources.containsKey(nodeId))
                amount = resources.get(nodeId).getLeft().getValue();
            else
                return true;
            toConsume = amount - excessOverflow;
            int outputCount = getOutputCount();
            int totalConsume = (toConsume / outputCount) * outputCount;
            if (toConsume % outputCount != 0)
                totalConsume += outputCount;
            int originalConsume = (amount / outputCount) * outputCount;
            if (amount % outputCount != 0)
                originalConsume += outputCount;
            int difference = (originalConsume - totalConsume) / outputCount;
            for (Node child : children) {
                if (!ingredients.containsKey(child.target)) {
                    if (!child.consumeResources(resources, state, excess, neededMap, 0))
                        return false;
                }
                else if (!child.consumeResources(resources, state, excess, neededMap, difference * ingredients.get(child.target).getCount()))
                    return false;
            }
            return true;
        }
        private int getChildNeededFactor(Node child, int num, int neededToCraft, HashMap<Item, ItemStack> ingredients, RecipeCraftingProcess process) {
            if (child instanceof WorkbenchNode || child instanceof ToolNode)
                return Math.min(1, num);
            return ((neededToCraft * ingredients.get(child.target).getCount()) / process.recipe.getOutput().getCount());
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            int neededToCraft = getNeededToCraft(numNeeded);
            for (Node child : children) {
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(getChildNeededFactor(child, numNeeded, neededToCraft, ingredients, process), getChildNeededFactor(child, actualNeeded, neededToCraft, ingredients, process), excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, res);
                mergeResources(res, childRes.getRight());
            }
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            if (children.size() == 0) {
                naiveMaxCraftable = 0;
                return;
            }
            int outputFactor = Integer.MAX_VALUE;
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            for (Node child : children) {
                if (child instanceof WorkbenchNode)
                    continue;
                int inputAmount = ingredients.get(child.target).getCount();
                int outputAmount = ((RecipeCraftingProcess) processes.get(0)).recipe.getOutput().getCount();
                int maxCraftable = child.naiveMaxCraftable;
                outputFactor = Math.min(outputFactor, outputAmount * (maxCraftable / inputAmount));
            }
            naiveMaxCraftable = outputFactor;
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            HashMap<Item, ItemStack> stackTypes = new HashMap<>();
            for (Ingredient ing : ((RecipeCraftingProcess)processes.get(0)).recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                if (stackTypes.containsKey(stack.getItem())) {
                    ItemStack value = stackTypes.get(stack.getItem());
                    value.setCount(value.getCount() + stack.getCount());
                }
                else {
                    stackTypes.put(stack.getItem(), stack.copy());
                }
            }
            return stackTypes;
        }
        @Override
        public int getOutputCount() {
            return ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
        }
        private int calculateCraftingOutput() {
            List<Ingredient> ingredients = ((RecipeCraftingProcess)processes.get(0)).recipe.getIngredients();
            HashMap<Item, ItemStack> collected = collectIngredients();
            int output = Integer.MAX_VALUE;
            for (Ingredient ing : ingredients) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack itemStack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                synchronized (totalInventoryAvailabilityMap) {
                    int outputFactor = Math.min(totalInventoryAvailabilityMap.getOrDefault(itemStack.getItem(), 0) / collected.get(itemStack.getItem()).getCount(), itemStack.getItem().getMaxCount()) * ((RecipeCraftingProcess) processes.get(0)).recipe.getOutput().getCount();
                    output = Math.min(output, outputFactor);
                }
            }
            return output;
        }
        private void adjustTotalAvailability(Recipe<?> recipe, int craftingOutput) {
            Item outputItem = recipe.getOutput().getItem();
            synchronized (totalInventoryAvailabilityMap) {
                totalInventoryAvailabilityMap.put(outputItem, totalInventoryAvailabilityMap.getOrDefault(outputItem, 0) + craftingOutput);
            }
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                synchronized (totalInventoryAvailabilityMap) {
                    totalInventoryAvailabilityMap.put(stack.getItem(), totalInventoryAvailabilityMap.getOrDefault(stack.getItem(), 0) - (stack.getCount() * craftingOutput) / recipe.getOutput().getCount());
                }
            }
        }
        private void arrangeRecipe(Recipe<?> recipe, int craftAmount, boolean useInventory) {
            if (MC.player.getRecipeBook().contains(recipe)) {
                MC.interactionManager.clickRecipe(MC.player.currentScreenHandler.syncId, ((RecipeCraftingProcess)processes.get(0)).recipe, craftAmount <= 1 ? false : true);
            }
            else {
                int craftingGridSize = useInventory ? 2 : 3;
                int width = 3;
                if (recipe instanceof ShapedRecipe) {
                    ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
                    width = shapedRecipe.getWidth();
                }
                int slotNumber = 0;
                for (Ingredient ing : recipe.getIngredients()) {
                    int currentCraftAmount = craftAmount;
                    if (ing.getMatchingStacks().length > 0) {
                        int slotX = slotNumber % width;
                        int slotY = slotNumber / width;
                        int slot = slotY * craftingGridSize + slotX + 1;
                        List<Slot> slots = MC.player.currentScreenHandler.slots;
                        for (int i = slots.size() - 37; i < slots.size(); i++) {
                            if (currentCraftAmount <= 0)
                                break;
                            if (slots.get(i).getStack().getItem().equals(ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length].getItem())) {
                                int slotAmount = slots.get(i).getStack().getCount();
                                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                                if (slotAmount <= currentCraftAmount) {
                                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, MC.player);
                                    currentCraftAmount -= slotAmount;
                                } else {
                                    for (int j = 0; j < currentCraftAmount; j++) {
                                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 1, SlotActionType.PICKUP, MC.player);
                                    }
                                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                                    currentCraftAmount = 0;
                                }
                            }
                        }
                    }
                    slotNumber++;
                }
            }
        }
        @Override
        public boolean execute() {
            if (Registry.ITEM.getId(target).getPath().contains("crossbow")) {
                System.out.println("reached");
            }
            boolean useInventory = false;
            if (!usingCraftingTable()) {
                if (((RecipeCraftingProcess) processes.get(0)).recipe.fits(2, 2)) {
                    containerManager.openInventory();
                    useInventory = true;
                }
                else {
                    Block craftingTable = Registry.BLOCK.get(new Identifier("minecraft", "crafting_table"));
                    BlockPos nearestCraftingTable = containerManager.getClosestToPlayer(craftingTable);
                    if (nearestCraftingTable != null)
                        containerManager.navigateAndOpenContainer(nearestCraftingTable);
                }
            }
            int neededToCraft = getNeededToCraft(needed);
            int craftingOutput = 0;
            while ((craftingOutput = calculateCraftingOutput()) <= neededToCraft && craftingOutput > 0) {
                if (!usingCraftingTable() && !usingInventory()) return false;
                arrangeRecipe(((RecipeCraftingProcess) processes.get(0)).recipe, craftingOutput / ((RecipeCraftingProcess) processes.get(0)).recipe.getOutput().getCount(), useInventory);
                if (!awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false))
                    return false;
                if (!usingCraftingTable() && !usingInventory()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false);
                adjustTotalAvailability(((RecipeCraftingProcess)processes.get(0)).recipe, craftingOutput);
                neededToCraft -= craftingOutput;
            }
            for (int i = 0; i < neededToCraft / ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(); i++) {
                if (!usingCraftingTable() && !usingInventory()) return false;
                arrangeRecipe(((RecipeCraftingProcess) processes.get(0)).recipe, 1, useInventory);
                if (!awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false))
                    return false;
                if (!usingCraftingTable() && !usingInventory()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false);
                adjustTotalAvailability(((RecipeCraftingProcess)processes.get(0)).recipe, ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount());
            }
            //containerManager.closeScreen();
            return true;
        }
    }

    private class ChoiceNode extends Node {
        public ChoiceNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            List<Node> res = new ArrayList<>();
            for (CraftingProcess process : processes) {
                int m = process.getMultiplicity();
                for (int i = 0; i < m; i++) {
                    Node child = process.getNode().setStackShift(i);
                    res.add(child);
                }
            }
            return res;
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.0;
            for (Node child : children) {
                if (result.getOrDefault(child.nodeId, new Pair<>(new OperableInteger(), ResourceDomain.COMPOSITE)).getLeft().getValue() > 0)
                    executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.0;
            double minChild = Double.POSITIVE_INFINITY;
            for (Node child : children) {
                if (state.naiveEfficiencyMap.containsKey(child.nodeId))
                    minChild = Math.min(minChild, state.naiveEfficiencyMap.get(child.nodeId));
            }
            executionTime += minChild;
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected boolean consumeResourcesInternal(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            int toConsume = 0;
            if (resources.containsKey(nodeId))
                toConsume = resources.get(nodeId).getLeft().getValue() - excessOverflow;
            for (Node child : children) {
                if (toConsume == 0)
                    break;
                int numNeeded = 0;
                if (resources.containsKey(child.nodeId))
                    numNeeded = resources.get(child.nodeId).getLeft().getValue();
                int overflow = Math.max(0, numNeeded - toConsume);
                numNeeded = Math.min(toConsume, numNeeded);
                toConsume -= numNeeded;
                if (!child.consumeResources(resources, state, excess, neededMap, overflow))
                    return false;
            }
            return true;
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    Pair<OperableInteger, ResourceDomain> childItem = src.get(child.nodeId);
                    child.stackResourcesInternal(childItem.getLeft().getValue() * (num / item.getLeft().getValue()), dest, src);
                }
            }
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            int originalNumNeeded = numNeeded;
            Instant startTime = Instant.now();
            List<Pair<Node, Pair<Boolean, Resources<OperableInteger>>>> options = new ArrayList<>();
            CraftingState newState = state.clone();
            HashMap<Integer, Integer> newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
            HashMap<Item, HashMap<Integer, Integer>> newExcess = deepcopyExcess(excess);
            List<Node> prioritizedChildren = new ArrayList<>(children);
            Collections.sort(prioritizedChildren, Comparator.comparing(c -> state.naiveEfficiencyMap.getOrDefault(c.nodeId, 0.0)));
            double bestEfficiency = Double.POSITIVE_INFINITY;
            for (Node child : prioritizedChildren) {
                if (bestEfficiency < state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0))
                    break;
                if (child.naiveMaxCraftable == 0)
                    continue;
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(1, Math.min(1, actualNeeded), newExcess, newNeededMap, newState, useHeuristic);
                bestEfficiency = Math.min(bestEfficiency, newState.efficiencyMap.getOrDefault(child.nodeId, Double.POSITIVE_INFINITY));
                if (childRes.getLeft()) {
                    options.add(new Pair<>(child, childRes));
                    state.efficiencyMap = (HashMap<Integer, Double>) newState.efficiencyMap.clone();
                }
                newState = state.clone();
                newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
                newExcess = deepcopyExcess(excess);

            }
            if (options.size() == 0)
                return new Pair<>(actualNeeded == 0, res);
            Collections.sort(options, Comparator.comparing(c -> state.efficiencyMap.getOrDefault(c.getLeft().nodeId, 0.0)));
            newState = state.clone();
            newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
            newExcess = deepcopyExcess(excess);
            globalTimeTaken = globalTimeTaken.add(new BigInteger((Instant.now().getNano() - startTime.getNano()) + ""));
            Pair<Boolean, Resources<OperableInteger>> childRes = options.get(0).getRight();
            int amount = 0;
            while (numNeeded > 0 && options.size() > 0) {
                if (!childRes.getLeft()) {
                    options.remove(0);
                    if (options.size() == 0)
                        break;
                    childRes = options.get(0).getRight();
                    continue;
                }
                if (options.get(0).getLeft().consumeResources(childRes.getRight(), newState, newExcess, newNeededMap, 0)) {
                    numNeeded--;
                    actualNeeded = Math.max(0, actualNeeded - 1);
                    amount++;
                    if (amount > 0 && numNeeded == 0) {
                        //Resources<OperableInteger> toConsume = options.get(0).getLeft().getBaseResources(amount, excess, neededMap, state, useHeuristic).getRight();
                        Resources<OperableInteger> toConsume = options.get(0).getLeft().stackResources(amount, childRes.getRight());
                        options.get(0).getLeft().consumeResources(toConsume, state, excess, neededMap, 0);
                        mergeResources(res, toConsume);
                    }
                }
                else {
                    if (amount > 0) {
                        //Resources<OperableInteger> toConsume = options.get(0).getLeft().getBaseResources(amount, excess, neededMap, state, useHeuristic).getRight();
                        Resources<OperableInteger> toConsume = options.get(0).getLeft().stackResources(amount, childRes.getRight());
                        options.get(0).getLeft().consumeResources(toConsume, state, excess, neededMap, 0);
                        mergeResources(res, toConsume);
                    }
                    amount = 0;
                    newState = state.clone();
                    newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
                    newExcess = deepcopyExcess(excess);
                    childRes = options.get(0).getLeft().getBaseResources(1, Math.min(1, actualNeeded), excess, neededMap, state, useHeuristic);
                }
            }
            res.put(nodeId, new Pair<>(new OperableInteger(originalNumNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(actualNeeded == 0, res);
            /*if (useHeuristic)
                Collections.sort(orderedItems, Comparator.comparing(o -> o.efficiencyHeuristic));
            for (Node child : orderedItems) {
                if (numNeeded == 0)
                    break;
                if (child.naiveMaxCraftable == 0)
                    continue;
                CraftingState newState = state.clone();
                HashMap<Integer, Integer> newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
                HashMap<Item, HashMap<Integer, Integer>> newExcess = deepcopyExcess(excess);
                int amount = 0;
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(1, newExcess, newNeededMap, newState, useHeuristic);
                while (numNeeded > 0 && childRes.getLeft()) {
                    amount++;
                    numNeeded--;
                    childRes = child.getBaseResources(1, newExcess, newNeededMap, newState, useHeuristic);
                }
                res.putAll(child.getBaseResources(amount, excess, neededMap, state, useHeuristic).getRight());
            }
            return new Pair<>(numNeeded == 0, res);*/
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            for (Node child : children) {
                naiveMaxCraftable += child.naiveMaxCraftable;
            }
        }
        @Override
        protected boolean requiresAllChildren() {
            return false;
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class Resources<T extends ArithmeticOperable> {
        private HashMap<Integer, Pair<T, ResourceDomain>> resources;
        public Resources() {
            resources = new HashMap<>();
        }
        public void put(int nodeId, Pair<T, ResourceDomain> res) {
            resources.put(nodeId, res);
        }
        public Pair<T, ResourceDomain> get(int nodeId) {
            return resources.get(nodeId);
        }
        public Pair<T, ResourceDomain> getOrDefault(int nodeId, Pair<T, ResourceDomain> def) {
            return resources.getOrDefault(nodeId, def);
        }
        public boolean containsKey(int nodeId) {
            return resources.containsKey(nodeId);
        }
        public Set<Integer> keySet() {
            return resources.keySet();
        }
        public Resources<T> mult(int num) {
            Resources<T> res = new Resources<>();
            for (Integer item : resources.keySet()) {
                Pair<T, ResourceDomain> value = resources.get(item);
                res.put(item, new Pair<>((T) value.getLeft().mult(num), value.getRight()));
            }
            return res;
        }
        public Resources<OperableInteger> ceil() {
            Resources<OperableInteger> res = new Resources<>();
            for (Integer item : resources.keySet()) {
                Pair<T, ResourceDomain> value = resources.get(item);
                res.put(item, new Pair<>(new OperableInteger(value.getLeft().ceil()), value.getRight()));
            }
            return res;
        }
        public Resources<T> clone() {
            Resources<T> res = new Resources<>();
            for (Integer item : resources.keySet()) {
                Pair<T, ResourceDomain> value = resources.get(item);
                res.put(item, new Pair<>((T) value.getLeft().clone(), value.getRight()));
            }
            return res;
        }
    }

    private int gcd(int a, int b) {
        if (b == 0)
                return a;
        return gcd(b, a % b);
    }

    private interface ArithmeticOperable<T> {
        T add(T other);
        T mult(T other);
        T div(T other);
        T clone();
        int ceil();
    }

    private class OperableInteger implements ArithmeticOperable<OperableInteger> {
        private int value;

        public OperableInteger(int value) {
            this.value = value;
        }

        public OperableInteger() {
            this(0);
        }

        public OperableInteger add(OperableInteger other) {
            return new OperableInteger(value + other.value);
        }

        public OperableInteger mult(OperableInteger other) {
            return new OperableInteger(value * other.value);
        }

        public OperableInteger div(OperableInteger other) {
            return new OperableInteger(value / other.value);
        }

        public int ceil() {
            return getValue();
        }

        public int getValue() {
            return value;
        }

        public OperableInteger clone() {
            return new OperableInteger(value);
        }
    }

    private class Fraction implements ArithmeticOperable<Fraction> {
        private int numerator;
        private int denominator;

        public Fraction(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
            simplify();
        }

        public Fraction(int numerator) {
            this(numerator, 1);
        }

        private void simplify() {
            int d = gcd(numerator, denominator);
            numerator /= d;
            denominator /= d;
        }

        public Fraction add(Fraction other) {
            return new Fraction(numerator * other.denominator + other.numerator * denominator, other.denominator * denominator);
        }

        public Fraction mult(Fraction other) {
            return new Fraction(numerator * other.numerator, denominator * other.denominator);
        }

        public Fraction div(Fraction other) {
            return mult(other.reciprocal());
        }

        public Fraction reciprocal() {
            return new Fraction(denominator, numerator);
        }

        public int ceil() {
            if (denominator == 1)
                return numerator;
            return (int) Math.ceil((double) numerator / denominator);
        }

        public Fraction clone() {
            return new Fraction(numerator, denominator);
        }

        @Override
        public String toString() {
            return denominator > 1 ? numerator + "/" + denominator : numerator + "";
        }
    }

    private class InventoryNode extends Node {
        public InventoryNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.inventoryAvailability.getOrDefault(target, 0) > 0;
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.0;
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.0;
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.INVENTORY));
            state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) - actualNeeded);
            return new Pair<>(state.inventoryAvailability.get(target) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            naiveMaxCraftable = state.inventoryAvailability.getOrDefault(target, 0);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            return inventoryQuery.acquire(target, needed);
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class StorageNode extends Node {
        public StorageNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.storageAvailability.getOrDefault(target, 0) > 0;
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            int count = result.getOrDefault(nodeId, new Pair<>(new OperableInteger(), ResourceDomain.COMPOSITE)).getLeft().getValue();
            List<BlockPos> route = containerQuery.getAcquireRoute(target, count);
            Vec3d prevPos = MC.player.getPos();
            double totalDistance = 0;
            for (BlockPos pos : route) {
                Vec3d curPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                totalDistance += curPos.subtract(prevPos).length();
                prevPos = curPos;
            }
            double executionTime = totalDistance / MC.player.getMovementSpeed();
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = containerQuery.nearestContainerDistance / MC.player.getMovementSpeed();
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.STORAGE));
            state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) - actualNeeded);
            return new Pair<>(state.storageAvailability.get(target) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            naiveMaxCraftable = state.storageAvailability.getOrDefault(target, 0);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            return containerQuery.acquire(target, needed);
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class WorldNode extends Node {
        private Item dropped;
        private Block block;
        public WorldNode(Item dropped, Block block, int needed, List<CraftingProcess> processes) {
            super(dropped, needed, processes);
            this.dropped = dropped;
            this.block = block;
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    child.stackResourcesInternal(1, dest, src);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return List.of(new ToolCraftingProcess(((WorldCraftingProcess) processes.get(0)).block).getNode());
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) > 0;
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            int count = result.getOrDefault(nodeId, new Pair<>(new OperableInteger(), ResourceDomain.COMPOSITE)).getLeft().getValue();
            Block targetBlock = ((WorldCraftingProcess) processes.get(0)).block;
            double executionTime = nearestBlockDistanceMap.getOrDefault(targetBlock, 0.0) / MC.player.getMovementSpeed();
            executionTime += count * 100;
            if (block.getDefaultState().isToolRequired()) {
                for (Node child : children) {
                    executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
                }
            }
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            Block targetBlock = ((WorldCraftingProcess) processes.get(0)).block;
            double executionTime = nearestBlockDistanceMap.getOrDefault(targetBlock, 0.0) / MC.player.getMovementSpeed();
            if (block.getDefaultState().isToolRequired()) {
                for (Node child : children) {
                    executionTime += state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0);
                }
            }
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.WORLD));
            state.worldAvailability.put(block, state.worldAvailability.getOrDefault(block, 0) - actualNeeded);
            if (block.getDefaultState().isToolRequired()) {
                for (Node child : children) {
                    Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(1, Math.min(1, actualNeeded), excess, neededMap, state, useHeuristic);
                    if (!childRes.getLeft())
                        return new Pair<>(false, res);
                    mergeResources(res, childRes.getRight());
                }
            }
            return new Pair<>(state.worldAvailability.get(block) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            naiveMaxCraftable = state.worldAvailability.getOrDefault(block, 0);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            if (!block.getDefaultState().isToolRequired())
                return worldQuery.acquire(block, needed);
            if (!toolManager.canMine(block))
                return false;
            return worldQuery.acquire(block, needed);
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class PathingNode extends Node {
        public PathingNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            double executionTime = nearestBlockDistanceMap.getOrDefault(targetBlock, 0.0) / MC.player.getMovementSpeed();
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            double executionTime = nearestBlockDistanceMap.getOrDefault(targetBlock, 0.0) / MC.player.getMovementSpeed();
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            naiveMaxCraftable = state.worldAvailability.getOrDefault(targetBlock, 0);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            //Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            //return pathFinder.path(nearestBlockPosMap.getOrDefault(targetBlock, BlockPos.ORIGIN));
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            BlockPos nearestTarget = containerManager.getClosestToPlayer(targetBlock);
            if (nearestTarget != null)
                return containerManager.navigateAndOpenContainer(nearestTarget);
            if (!nearestBlockPosMap.containsKey(targetBlock))
                return false;
            nearestTarget = nearestBlockPosMap.get(targetBlock).up();
            return pathFinder.path(nearestTarget);
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class PlacementNode extends Node {
        public PlacementNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    child.stackResourcesInternal(1, dest, src);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            CraftingPlan plan = getCraftingPlan(Registry.ITEM.getId(target));
            List<CraftingProcess> processes = new ArrayList<>();
            for (CraftingProcess process : plan.processes) {
                if (!(process instanceof WorldCraftingProcess)) {
                    processes.add(process);
                }
            }
            return List.of(new ChoiceNode(target, 0, processes));
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.1;
            for (Node child : children) {
                executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.1;
            for (Node child : children) {
                executionTime += state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            for (Node child : children) {
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(numNeeded, actualNeeded, excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, new Resources<>());
                mergeResources(res, childRes.getRight());
            }
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            naiveMaxCraftable = 0;
            for (Node child : children) {
                naiveMaxCraftable += child.naiveMaxCraftable;
            }
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            BlockPos pos =  blockManager.getNearestPlaceablePosition();
            blockManager.placeBlock(target, pos);
            containerManager.addContainer(((PlacementCraftingProcess) processes.get(0)).block, pos);
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class WorkbenchNode extends Node {
        public WorkbenchNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    child.stackResourcesInternal(1, dest, src);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            List<CraftingProcess> processes = new ArrayList<>();
            processes.add(new PathingCraftingProcess(((WorkbenchCraftingProcess) this.processes.get(0)).block));
            processes.add(new PlacementCraftingProcess(((WorkbenchCraftingProcess) this.processes.get(0)).block));
            return List.of(new ChoiceNode(target, 0, processes));
        }
        @Override
        protected boolean consumeResourcesInternal(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            //Pair<OperableInteger, ResourceDomain> res = resources.getOrDefault(nodeId, new Pair<>(new OperableInteger(), ResourceDomain.COMPOSITE));
            //res.setLeft(new OperableInteger());
            //resources.put(nodeId, res);
            state.deadNodes.add(nodeId);
            for (Node child : children) {
                if (!child.consumeResources(resources, state, excess, neededMap, excessOverflow))
                    return false;
            }
            return true;
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.0;
            for (Node child : children) {
                executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.0;
            for (Node child : children) {
                executionTime += state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            for (Node child : children) {
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(numNeeded, actualNeeded, excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, new Resources<>());
                mergeResources(res, childRes.getRight());
            }
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            naiveMaxCraftable = 0;
            for (Node child : children) {
                naiveMaxCraftable += child.naiveMaxCraftable;
            }
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private class ToolNode extends Node {
        public ToolNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected void stackResourcesInternal(int num, Resources<OperableInteger> dest, Resources<OperableInteger> src) {
            if (!src.containsKey(nodeId))
                return;
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            for (Node child : children) {
                if (src.containsKey(child.nodeId)) {
                    child.stackResourcesInternal(1, dest, src);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            Set<Item> matchingTools = toolManager.getMatchingTools(((ToolCraftingProcess) processes.get(0)).block);
            List<CraftingProcess> toolProcesses = new ArrayList<>();
            for (Item tool : matchingTools) {
                CraftingPlan plan = getCraftingPlan(Registry.ITEM.getId(tool));
                for (CraftingProcess process : plan.processes) {
                    if (nodes.contains(process.getNode()))
                        continue;
                    toolProcesses.add(process);
                }
            }
            return List.of(new ChoiceNode(target, 0, toolProcesses));
        }
        @Override
        protected boolean consumeResourcesInternal(Resources<OperableInteger> resources, CraftingState state, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, int excessOverflow) {
            //Pair<OperableInteger, ResourceDomain> res = resources.getOrDefault(nodeId, new Pair<>(new OperableInteger(), ResourceDomain.COMPOSITE));
            //res.setLeft(new OperableInteger());
            //resources.put(nodeId, res);
            state.deadNodes.add(nodeId);
            for (Node child : children) {
                if (!child.consumeResources(resources, state, excess, neededMap, excessOverflow))
                    return false;
            }
            return true;
        }
        @Override
        protected void calculateExecutionTime(Resources<OperableInteger> result, CraftingState state) {
            double executionTime = 0.0;
            for (Node child : children) {
                executionTime += state.efficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.efficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected void calculateLowerEfficiencyBound(CraftingState state) {
            double executionTime = 0.0;
            for (Node child : children) {
                executionTime += state.naiveEfficiencyMap.getOrDefault(child.nodeId, 0.0);
            }
            state.naiveEfficiencyMap.put(nodeId, executionTime);
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(int numNeeded, int actualNeeded, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic, HashMap<Item, HashMap<Integer, Integer>> excess) {
            Resources<OperableInteger> res = new Resources<>();
            if (children.size() == 0)
                return new Pair<>(true, res);
            Set<Item> possibleTools = toolManager.getMatchingTools(((ToolCraftingProcess) processes.get(0)).block);
            for (Integer id : state.toolAvailability.keySet()) {
                if (id <= nodeId && possibleTools.contains(state.toolAvailability.get(id))) {
                    return new Pair<>(true, res);
                }
            }
            for (Item item : state.inventoryAvailability.keySet()) {
                if (possibleTools.contains(item) && state.inventoryAvailability.get(item) > 0) {
                    state.inventoryAvailability.put(item, state.inventoryAvailability.get(item) - 1);
                    state.toolAvailability.put(nodeId, item);
                }
            }
            for (Node child : children) {
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(numNeeded, actualNeeded, excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, new Resources<>());
                mergeResources(res, childRes.getRight());
            }
            for (Node n : children.get(0).children) {
                if (neededMap.getOrDefault(n.nodeId, 0) > 0) {
                    state.toolAvailability.put(nodeId, n.target);
                    break;
                }
            }
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            naiveMaxCraftable = 0;
            for (Node child : children) {
                naiveMaxCraftable += child.naiveMaxCraftable;
            }
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private boolean usingCraftingTable() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof CraftingScreenHandler;
    }

    private boolean usingFurnace() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof FurnaceScreenHandler;
    }

    private boolean usingInventory() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof PlayerScreenHandler;
    }

    private Node makeRootNode(Identifier identifier) {
        return getCraftingPlan(identifier).getNode();
    }

    private class CraftingQueueEntry {
        private Identifier itemId;
        private int count;
        private boolean craftAll;
        public CraftingQueueEntry(Identifier itemId, int count, boolean craftAll) {
            this.itemId = itemId;
            this.count = count;
            this.craftAll = craftAll;
        }
    }

    private enum ResourceDomain {
        INVENTORY, STORAGE, WORLD, COMPOSITE
    }

    public void queueCraft(Identifier itemId, int count, boolean craftAll) {
        synchronized(craftingQueue) {
            craftingQueue.add(new CraftingQueueEntry(itemId, count, craftAll));
        }
    }

    private void updateTotalInventoryAvailability() {
        NotifyingRunnable updateRunnable = new NotifyingRunnable() {
            @Override
            protected void runInternal() {
                synchronized (totalInventoryAvailabilityMap) {
                    totalInventoryAvailabilityMap = inventoryQuery.getAvailabilityMap();
                }
            }
        };
        updateRunnable.runUntilDone();
    }

    public void notifySlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
        NotifyingRunnable updateRunnable = new NotifyingRunnable() {
            @Override
            protected void runInternal() {
                synchronized (totalInventoryAvailabilityMap) {
                    totalInventoryAvailabilityMap = inventoryQuery.getAvailabilityMap();
                }
            }
        };
        updateRunnable.runWithoutWaiting();
        slotUpdateLock.lock();
        try {
            latestSlotUpdate = new SlotUpdateInfo(packet.getSlot(), packet.getItemStack());
            slotUpdateCondition.signalAll();
        } finally {
            slotUpdateLock.unlock();
        }
    }

    private void awaitContainerOpen() {
        containerOpenLock.lock();
        try {
            containerOpenCondition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            containerOpenLock.unlock();
        }
    }

    private boolean isStorageContainerHandler(ScreenHandler handler) {
        return handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler;
    }

    public void storageContainerAccessed(int syncId) { }

    public void storageContainerContent(int syncId, List<ItemStack> content) {
        if (isStorageContainerHandler(MC.player.currentScreenHandler)) {
            HashMap<Item, Integer> contentMap = new HashMap<>();
            for (int i = 0; i < content.size() - 36; i++) {
                ItemStack stack = content.get(i);
                if (stack.getCount() > 0)
                    contentMap.put(stack.getItem(), contentMap.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
            containerQuery.updateContainer(contentMap, latestBlockPos);
            containerManager.addContainer(MC.world.getBlockState(latestBlockPos).getBlock(), latestBlockPos);
        }
        containerOpenLock.lock();
        try {
            containerOpenCondition.signalAll();
        }
        finally {
            containerOpenLock.unlock();
        }
    }

    public void storageContainerClosed() {
        containerManager.currentContainer = null;
        if (!isStorageContainerHandler(MC.player.currentScreenHandler))
            return;
        if (!isCurrentlyCrafting) {
            containerQuery.updateContainer(inventoryQuery.getCurrentContainerAvailabilityMap(), latestBlockPos);
        }
    }

    public void blockPositionClicked(BlockPos pos) {
        latestBlockPos = pos;
    }

    private void craft(Identifier itemId, int count, boolean craftAll) {
        isCurrentlyCrafting = true;
        new Thread(() -> {
            containerManager.updateContainers(worldQuery.getLocations(containerBlockTypes));
            Node root = makeRootNode(itemId);
            Item item = Registry.ITEM.get(itemId);
            int initialCount = inventoryAvailabilityMap.getOrDefault(item, 0);
            inventoryAvailabilityMap.put(item, 0);
            root.craft(craftAll ? Integer.MAX_VALUE : count);
            inventoryAvailabilityMap.put(item, inventoryAvailabilityMap.getOrDefault(item, 0) + initialCount);
            /*Item item = Registry.ITEM.get(itemId);
            int initialCount = inventoryAvailabilityMap.getOrDefault(item, 0);
            inventoryAvailabilityMap.put(item, 0);
            if (craftAll) {
                root = makeRootNode(itemId, getMaxCraftable(itemId, CraftingParams.VERIFY_AND_GENERATE, new CraftingState(inventoryAvailabilityMap, storageAvailabilityMap, worldAvailabilityMap, new HashSet<>())));
            }
            CraftingState verificationState = root.verify();
            if (verificationState.success) {
                HashMap<Item, Integer> obtainFromStorage = new HashMap<>();
                for (StorageNode node : verificationState.storageNodes) {
                    obtainFromStorage.put(node.target, obtainFromStorage.getOrDefault(node.target, 0) + node.needed);
                    storageAvailabilityMap.put(node.target, storageAvailabilityMap.getOrDefault(node.target, 0) - node.needed);
                }
                HashMap<Item, Integer> original = (HashMap<Item, Integer>) obtainFromStorage.clone();
                containerQuery.acquire(obtainFromStorage);
                for (Item originalItem : original.keySet()) {
                    inventoryAvailabilityMap.put(originalItem, inventoryAvailabilityMap.getOrDefault(originalItem, 0) + (original.get(originalItem) - obtainFromStorage.getOrDefault(originalItem, 0)));
                }
                if (!root.craft().success) {
                    int finalCount = totalInventoryAvailabilityMap.getOrDefault(item, 0);
                    synchronized (craftingQueue) {
                        craftingQueue.remove(0);
                        //craftingQueue.get(0).count -= finalCount - initialCount;
                        //if (craftingQueue.get(0).count <= 0)
                        //    craftingQueue.remove(0);
                    }
                    inventoryAvailabilityMap.put(item, inventoryAvailabilityMap.getOrDefault(item, 0) + initialCount);
                    isCurrentlyCrafting = false;
                    return;
                }
            }
            inventoryAvailabilityMap.put(item, inventoryAvailabilityMap.getOrDefault(item, 0) + initialCount);*/
            synchronized(craftingQueue) {
                craftingQueue.remove(0);
            }
            isCurrentlyCrafting = false;
        }).start();
    }

    @Override
    public void onEnable() {
        if (isBaritoneAPIInstalled()) {
            baritoneInterface = new BaritoneInterface();
            pathFinder = new BaritonePathfinder();
        }
        else {
            pathFinder = new WurstPathfinder();
        }
        if (processMap == null)
            initProcessMap();
        if (containerBlockTypes == null)
            initContainerBlockTypes();
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onUpdate() {
        synchronized (craftingQueue) {
            if (!isCurrentlyCrafting) {
                if (craftingQueue.size() > 0) {
                    CraftingQueueEntry entry = craftingQueue.get(0);
                    if (doneCrafting) {
                        synchronized (totalInventoryAvailabilityMap) {
                            totalInventoryAvailabilityMap = inventoryQuery.getAvailabilityMap();
                            inventoryAvailabilityMap = (HashMap<Item, Integer>) totalInventoryAvailabilityMap.clone();
                        }
                        storageAvailabilityMap = containerQuery.getAvailabilityMap();
                        worldAvailabilityMap = worldQuery.getAvailabilityMap();
                        nearestBlockPosMap = worldQuery.getNearestPositions();
                        nearestBlockDistanceMap = worldQuery.getNearestBlockDistances(nearestBlockPosMap);
                        doneCrafting = false;
                    }
                    craft(entry.itemId, entry.count, entry.craftAll);
                }
                else {
                    doneCrafting = true;
                }
            }
        }
    }
}
