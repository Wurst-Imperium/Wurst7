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
            if (recipeType != RecipeType.CRAFTING)
                continue;
            String path = id.getPath();
            if (path.contains("_from_")) {
                String[] components = path.split("_from_");
                String sourceId = components[0];
                Identifier baseId = new Identifier(id.getNamespace(), sourceId);
                if (!processMap.containsKey(baseId))
                    processMap.put(baseId, new ArrayList<>());
                processMap.get(baseId).add(new RecipeCraftingProcess(recipe));
            }
            else {
                if (!processMap.containsKey(id))
                    processMap.put(id, new ArrayList<>());
                processMap.get(id).add(new RecipeCraftingProcess(recipe));
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
        public ContainerStorageQuery() {
            containers = new HashMap<>();
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
                    containerManager.goToContainer(pos);
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
                        containerManager.goToContainer(pos);
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
        public void goToContainer(BlockPos container) {
            if (container.equals(currentContainer))
                return;
            if (MC.currentScreen != null)
                closeScreen();
            BlockPos nearestPathablePosition = getNearestPathablePosition(container);
            if (nearestPathablePosition == null)
                nearestPathablePosition = container.up();
            pathFinder.path(nearestPathablePosition);
            IMC.getInteractionManager().rightClickBlock(container, Direction.NORTH, Vec3d.ZERO);
            awaitContainerOpen();
            currentContainer = container;
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
        private HashSet<Node> visited;
        private List<StorageNode> storageNodes = new ArrayList<>();
        private HashMap<Item, Integer> craftingItemFrequency = new HashMap<>();
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
        protected VerificationInfo verificationInfo;
        protected String lpVarName = null;
        protected int naiveMaxCraftable;
        protected int maxCraftable;
        protected int nodeId;
        private static int maxNodeId = 0;
        protected double efficiencyHeuristic;
        public Node(Item target, int needed, List<CraftingProcess> processes) {
            nodeId = maxNodeId++;
            children = new ArrayList<>();
            this.processes = processes;
            this.target = target;
            this.needed = needed;
            stackShift = 0;
        }
        public List<Pair<ItemStack, ResourceDomain>> toItemStack(Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> res) {
            List<Pair<ItemStack, ResourceDomain>> result = new ArrayList<>();
            if (res.getRight().containsKey(nodeId)) {
                result.add(new Pair<>(new ItemStack(target, res.getRight().get(nodeId).getLeft()), res.getRight().get(nodeId).getRight()));
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
        private void generateTree(HashSet<Node> visited, CraftingState state) {
            if (shouldRememberVisit())
                visited.add(this);
            children = getChildren(visited, state);
            for (Node child : children) {
                child.generateTree(visited, state);
            }
            if (shouldRememberVisit())
                visited.remove(this);
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
                if ((child.shouldRememberVisit() && nodes.contains(child)) || !child.canPossiblyCraft(state))
                    continue;
                res.add(child);
            }
            return res;
        }
        protected boolean canPossiblyCraft(CraftingState state) {
            return true;
        }
        protected abstract void calculateEfficiencyHeuristic(int amount);
        private CraftingState createFreshState() {
            HashMap<Item, Integer> inventoryAvailability = (HashMap<Item, Integer>) inventoryAvailabilityMap.clone();
            HashMap<Item, Integer> storageAvailability = (HashMap<Item, Integer>) storageAvailabilityMap.clone();
            HashMap<Block, Integer> worldAvailability = (HashMap<Block, Integer>) worldAvailabilityMap.clone();
            CraftingState state = new CraftingState(inventoryAvailability, storageAvailability, worldAvailability, new HashSet<>(), new HashMap<>());
            return state;
        }
        private int calculateMaxCraftableInternal(int upperBound, boolean useHeuristic, boolean setMaxCraftable, boolean populateNaiveMaxCraftable) {
            CraftingState state = createFreshState();
            if (populateNaiveMaxCraftable)
                genNaiveMaxCraftable(state.clone());
            HashMap<Integer, Integer> neededMap = new HashMap<>();
            HashMap<Integer, Integer> newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
            CraftingState newState = state.clone();
            HashMap<Item, HashMap<Integer, Integer>> excess = new HashMap<>();
            Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> resources = getBaseResources(1, excess, newNeededMap, newState, useHeuristic);
            int amount = 0;
            while (amount < upperBound && resources.getLeft()) {
                amount++;
                neededMap = newNeededMap;
                newNeededMap = (HashMap<Integer, Integer>) neededMap.clone();
                state = newState;
                newState = state.clone();
                resources = getBaseResources(1, excess, newNeededMap, newState, useHeuristic);
            }
            applyNeededMap(neededMap);
            if (setMaxCraftable)
                applyMaxCraftableMap(neededMap);
            return amount;
        }
        private void print(int indent, boolean ignoreNeeded) {
            if ((ignoreNeeded && naiveMaxCraftable > 0) || needed > 0) {
                String indentation = "";
                for (int i = 0; i < indent; i++) {
                    indentation += " ";
                }
                System.out.println(indentation + target + ": " + needed + ", " + efficiencyHeuristic + ", " + this.getClass());
                for (Node child : children) {
                    child.print(indent + 4, ignoreNeeded);
                }
            }
        }
        private int calculateMaxCraftable(int upperBound) {
            generateTree(new HashSet<>(), createFreshState());
            reorderNodeIds(0);
            int amount = calculateMaxCraftableInternal(upperBound, false, true, true);
            calculateEfficiencyHeuristic(Math.min(amount, upperBound));
            calculateMaxCraftableInternal(upperBound, true, false, false);
            print(0, false);
            System.out.println("Craftable: " + amount);
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
        private Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResources(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            if (excess.containsKey(target)) {
                HashMap<Integer, Integer> itemMap = excess.get(target);
                for (int id : itemMap.keySet()) {
                    if (id <= nodeId) {
                        int reductionFactor = Math.min(numNeeded, itemMap.get(id));
                        numNeeded -= reductionFactor;
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
            if (numNeeded > 0)
                return getBaseResourcesInternal(numNeeded, excess, neededMap, state, useHeuristic);
            return new Pair<>(true, new HashMap<>());
        }
        protected abstract Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic);
        protected void genNaiveMaxCraftable(CraftingState state) {
            for (Node child : children) {
                child.genNaiveMaxCraftable(state);
            }
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

    private class RecipeNode extends Node {
        public RecipeNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
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
        protected void calculateEfficiencyHeuristic(int amount) {
            efficiencyHeuristic = 0.05;
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            for (Node child : children) {
                if (child instanceof WorkbenchNode)
                    child.calculateEfficiencyHeuristic(1);
                else
                    child.calculateEfficiencyHeuristic((int)Math.ceil((double)(amount * ingredients.get(child.target).getCount()) / process.recipe.getOutput().getCount()));
                efficiencyHeuristic += child.efficiencyHeuristic;
            }
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            HashMap<Item, ItemStack> ingredients = collectIngredients();
            int neededToCraft = getNeededToCraft(numNeeded);
            for (Node child : children) {
                Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> childRes = child.getBaseResources(child instanceof WorkbenchNode ? 1 : ((neededToCraft * ingredients.get(child.target).getCount()) / process.recipe.getOutput().getCount()), excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, res);
                res.putAll(childRes.getRight());
            }
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
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
                        containerManager.goToContainer(nearestCraftingTable);
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
        protected void calculateEfficiencyHeuristic(int amount) {
            efficiencyHeuristic = 0.0;
            List<Pair<Node, Double>> timePerItem = new ArrayList<>();
            int smallestNaiveMaxCraftable = Integer.MAX_VALUE;
            for (Node child : children) {
                if (child.naiveMaxCraftable == 0)
                    continue;
                if (child.naiveMaxCraftable < smallestNaiveMaxCraftable)
                    smallestNaiveMaxCraftable = child.naiveMaxCraftable;
            }
            for (Node child : children) {
                if (child.naiveMaxCraftable == 0)
                    continue;
                int amt = Math.min(amount, smallestNaiveMaxCraftable);
                child.calculateEfficiencyHeuristic(amt);
                timePerItem.add(new Pair<>(child, child.efficiencyHeuristic / amt));
            }
            Collections.sort(timePerItem, Comparator.comparing(o -> o.getRight()));
            for (Pair<Node, Double> p : timePerItem) {
                if (amount == 0)
                    break;
                int m = Math.min(amount, smallestNaiveMaxCraftable);
                amount -= m;
                efficiencyHeuristic += m * p.getRight();
            }
        }
        private HashMap<Item, HashMap<Integer, Integer>> deepcopyExcess(HashMap<Item, HashMap<Integer, Integer>> excess) {
            HashMap<Item, HashMap<Integer, Integer>> res = new HashMap<>();
            for (Item item : excess.keySet()) {
                res.put(item, (HashMap<Integer, Integer>) excess.get(item).clone());
            }
            return res;
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            List<Node> orderedItems = new ArrayList<>(children);
            if (useHeuristic)
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
                Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> childRes = child.getBaseResources(1, newExcess, newNeededMap, newState, useHeuristic);
                while (numNeeded > 0 && childRes.getLeft()) {
                    amount++;
                    numNeeded--;
                    childRes = child.getBaseResources(1, newExcess, newNeededMap, newState, useHeuristic);
                }
                res.putAll(child.getBaseResources(amount, excess, neededMap, state, useHeuristic).getRight());
            }
            return new Pair<>(numNeeded == 0, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
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
            return 0;
        }
    }

    private class InventoryNode extends Node {
        public InventoryNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
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
        protected void calculateEfficiencyHeuristic(int amount) {
            efficiencyHeuristic = 0.0;
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            res.put(nodeId, new Pair<>(numNeeded, ResourceDomain.INVENTORY));
            state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) - numNeeded);
            return new Pair<>(state.inventoryAvailability.get(target) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
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
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.storageAvailability.getOrDefault(target, 0) > 0;
        }
        @Override
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected void calculateEfficiencyHeuristic(int amount) {
            List<BlockPos> route = containerQuery.getAcquireRoute(target, amount);
            Vec3d prevPos = MC.player.getPos();
            double totalDistance = 0;
            for (BlockPos pos : route) {
                Vec3d curPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                totalDistance += curPos.subtract(prevPos).length();
                prevPos = curPos;
            }
            efficiencyHeuristic = totalDistance / MC.player.getMovementSpeed();
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            res.put(nodeId, new Pair<>(numNeeded, ResourceDomain.STORAGE));
            state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) - numNeeded);
            return new Pair<>(state.storageAvailability.get(target) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
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
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return List.of(new ToolCraftingProcess(((WorldCraftingProcess) processes.get(0)).block).getNode());
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) > 0;
        }
        @Override
        protected void calculateEfficiencyHeuristic(int amount) {
            Vec3d playerPos = MC.player.getPos();
            Block targetBlock = ((WorldCraftingProcess) processes.get(0)).block;
            BlockPos nearestPos = nearestBlockPosMap.getOrDefault(targetBlock, BlockPos.ORIGIN);
            efficiencyHeuristic = new Vec3d(nearestPos.getX(), nearestPos.getY(), nearestPos.getZ()).subtract(playerPos).length() / MC.player.getMovementSpeed();
            efficiencyHeuristic += amount;
            if (block.getDefaultState().isToolRequired()) {
                for (Node child : children) {
                    child.calculateEfficiencyHeuristic(amount);
                    efficiencyHeuristic += child.efficiencyHeuristic;
                }
            }
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            res.put(nodeId, new Pair<>(numNeeded, ResourceDomain.WORLD));
            state.worldAvailability.put(block, state.worldAvailability.getOrDefault(block, 0) - numNeeded);
            if (block.getDefaultState().isToolRequired()) {
                for (Node child : children) {
                    Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> childRes = child.getBaseResources(1, excess, neededMap, state, useHeuristic);
                    if (!childRes.getLeft())
                        return new Pair<>(false, res);
                    res.putAll(childRes.getRight());
                }
            }
            return new Pair<>(state.worldAvailability.get(block) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
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
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected void calculateEfficiencyHeuristic(int amount) {
            Vec3d playerPos = MC.player.getPos();
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            BlockPos nearestPos = nearestBlockPosMap.getOrDefault(targetBlock, BlockPos.ORIGIN);
            efficiencyHeuristic = new Vec3d(nearestPos.getX(), nearestPos.getY(), nearestPos.getZ()).subtract(playerPos).length() / MC.player.getMovementSpeed();
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            return new Pair<>(true, new HashMap<>());
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
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
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            return pathFinder.path(nearestBlockPosMap.getOrDefault(targetBlock, BlockPos.ORIGIN));
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
        protected void calculateEfficiencyHeuristic(int amount) {
            efficiencyHeuristic = 0.1;
            for (Node child : children) {
                child.calculateEfficiencyHeuristic(amount);
                efficiencyHeuristic += child.efficiencyHeuristic;
            }
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            for (Node child : children) {
                Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> childRes = child.getBaseResources(numNeeded, excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, new HashMap<>());
                res.putAll(childRes.getRight());
            }
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
            naiveMaxCraftable = 0;
            for (Node child : children) {
                child.genNaiveMaxCraftable(state);
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
        protected List<Node> getChildrenInternal(HashSet<Node> nodes) {
            List<CraftingProcess> processes = new ArrayList<>();
            processes.add(new PathingCraftingProcess(((WorkbenchCraftingProcess) this.processes.get(0)).block));
            processes.add(new PlacementCraftingProcess(((WorkbenchCraftingProcess) this.processes.get(0)).block));
            return List.of(new ChoiceNode(target, 0, processes));
        }
        @Override
        protected void calculateEfficiencyHeuristic(int amount) {
            efficiencyHeuristic = 0.0;
            for (Node child : children) {
                child.calculateEfficiencyHeuristic(amount);
                efficiencyHeuristic += child.efficiencyHeuristic;
            }
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
            for (Node child : children) {
                Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> childRes = child.getBaseResources(numNeeded, excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, new HashMap<>());
                res.putAll(childRes.getRight());
            }
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
            naiveMaxCraftable = 0;
            for (Node child : children) {
                child.genNaiveMaxCraftable(state);
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
        protected void calculateEfficiencyHeuristic(int amount) {
            efficiencyHeuristic = 0.0;
            for (Node child : children) {
                child.calculateEfficiencyHeuristic(1);
                efficiencyHeuristic += child.efficiencyHeuristic;
            }
        }
        @Override
        protected Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> getBaseResourcesInternal(int numNeeded, HashMap<Item, HashMap<Integer, Integer>> excess, HashMap<Integer, Integer> neededMap, CraftingState state, boolean useHeuristic) {
            HashMap<Integer, Pair<Integer, ResourceDomain>> res = new HashMap<>();
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
                Pair<Boolean, HashMap<Integer, Pair<Integer, ResourceDomain>>> childRes = child.getBaseResources(numNeeded, excess, neededMap, state, useHeuristic);
                if (!childRes.getLeft())
                    return new Pair<>(false, new HashMap<>());
                res.putAll(childRes.getRight());
            }
            for (Node n : children.get(0).children) {
                if (neededMap.getOrDefault(n.nodeId, 0) > 0) {
                    state.toolAvailability.put(nodeId, n.target);
                    break;
                }
            }
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftable(CraftingState state) {
            super.genNaiveMaxCraftable(state);
            naiveMaxCraftable = 0;
            for (Node child : children) {
                child.genNaiveMaxCraftable(state);
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
        INVENTORY, STORAGE, WORLD
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
