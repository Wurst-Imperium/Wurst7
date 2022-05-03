package net.wurstclient.hacks;

import io.netty.util.concurrent.ThreadPerTaskExecutor;
import net.fabricmc.fabric.api.tool.attribute.v1.ToolManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.LootConditionManager;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.resource.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.wurstclient.Category;
import net.wurstclient.WurstClient;
import net.wurstclient.commands.GoToCmd;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.lwjgl.system.CallbackI;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.wurstclient.WurstClient.IMC;

public class AutoCraftHack extends Hack implements UpdateListener {

    private LinkedHashMap<Identifier, List<CraftingProcess>> processMap = null;
    private LinkedHashMap<String, Set<Item>> groups = null;

    private List<CraftingQueueEntry> craftingQueue = new ArrayList<>();

    private boolean isCurrentlyCrafting = false;

    private ReentrantLock slotUpdateLock = new ReentrantLock();
    private Condition slotUpdateCondition = slotUpdateLock.newCondition();

    private ReentrantLock containerOpenLock = new ReentrantLock();
    private Condition containerOpenCondition = containerOpenLock.newCondition();

    private LinkedHashMap<Integer, SlotUpdateInfo> latestSlotUpdates = new LinkedHashMap<>();
    private int lastSlotUpdatePacketRevision = -1;

    private InventoryStorageQuery inventoryQuery = new InventoryStorageQuery();
    private ContainerStorageQuery containerQuery = new ContainerStorageQuery();
    private WorldStorageQuery worldQuery = new WorldStorageQuery();

    private LinkedHashMap<Item, Integer> inventoryAvailabilityMap = new LinkedHashMap<>();
    private LinkedHashMap<Item, Integer> storageAvailabilityMap = new LinkedHashMap<>();
    private LinkedHashMap<Block, Integer> worldAvailabilityMap = new LinkedHashMap<>();
    private LinkedHashMap<Block, BlockPos> nearestBlockPosMap = new LinkedHashMap<>();
    private LinkedHashMap<Block, Double> nearestBlockDistanceMap = new LinkedHashMap<>();

    private Octree blockTree;

    private LinkedHashMap<Item, Integer> totalInventoryAvailabilityMap = new LinkedHashMap<>();

    private LinkedHashSet<Block> containerBlockTypes;

    private WurstPathfinder wurstPathfinder;
    private BaritoneChatPathfinder baritoneChatPathfinder;
    private BaritoneChatInterface baritoneChatInterface;
    private CheckboxSetting baritoneChatInterfaceEnabled = new CheckboxSetting("Baritone Chat Interface", "Use Baritone to pathfind and mine stuff (requires Baritone to be installed as a fabric mod).", false);
    private CheckboxSetting flightPathsEnabled = new CheckboxSetting("Flight Paths", "Navigate using Wurst's flight path processor whenever possible.", false);

    private BlockPos latestBlockPos = BlockPos.ORIGIN;

    private boolean doneCrafting = true;

    private ContainerManager containerManager = new ContainerManager();
    private ToolCraftingManager toolCraftingManager = new ToolCraftingManager();
    private BlockManager blockManager = new BlockManager();
    private InventoryManager inventoryManager = new InventoryManager();

    BigInteger globalTimeTaken = BigInteger.ZERO;

    private final List<WurstRunnable> runnableQueue = new ArrayList<>();

    public AutoCraftHack() {
        super("AutoCraft");
        setCategory(Category.ITEMS);
        addSetting(baritoneChatInterfaceEnabled);
        addSetting(flightPathsEnabled);
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
        containerBlockTypes = new LinkedHashSet<>();
        List<String> blockTypes = List.of("crafting_table", "chest", "furnace", "smithing_table");
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
        else if (type == RecipeType.SMITHING) {
            return new SmithingCraftingProcess(recipe);
        }
        return null;
    }

    public Set<String> getGroupNames() {
        if (groups == null)
            return new LinkedHashSet<>();
        return groups.keySet();
    }

    private void initGroups() {
        groups = new LinkedHashMap<>();
        LinkedHashSet<Item> foodGroup = new LinkedHashSet<>();
        for (Identifier id : Registry.ITEM.getIds()) {
            Item item = Registry.ITEM.get(id);
            if (item.isFood()) {
                if (!item.getFoodComponent().getStatusEffects().stream().map(instance -> instance.getFirst().getEffectType()).collect(Collectors.toList()).contains(StatusEffects.POISON))
                    foodGroup.add(item);
            }
        }
        groups.put("food", foodGroup);
    }

    public LootManager makeLootManager() {
        ResourcePackManager rpl = new ResourcePackManager(ResourceType.SERVER_DATA, new VanillaDataPackProvider());
        rpl.scanPacks();
        ResourcePack thePack = rpl.getProfiles().iterator().next().createResourcePack();
        ReloadableResourceManager resourceManager = new ReloadableResourceManagerImpl(ResourceType.SERVER_DATA);
        LootManager manager = new LootManager(new LootConditionManager());
        resourceManager.registerReloader(manager);
        try {
            resourceManager.reload(new ThreadPerTaskExecutor(Thread::new), new ThreadPerTaskExecutor(Thread::new), Collections.singletonList(thePack), CompletableFuture.completedFuture(Unit.INSTANCE)).get();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return manager;
    }

    private double blockBreakingTime(Block block, @Nullable ToolItem tool) {
        double speedMultiplier = 1.0;
        boolean canHarvest = !block.getDefaultState().isToolRequired();
        if (tool != null) {
            canHarvest |= ToolManager.handleIsEffectiveOn(block.getDefaultState(), new ItemStack(tool, 1), null);
            if (canHarvest) {
                speedMultiplier = ToolManager.handleBreakingSpeed(block.getDefaultState(), new ItemStack(tool, 1), null);
            }
            else {
                speedMultiplier = 1.0;
            }
        }
        double damage = speedMultiplier / block.getHardness();
        if (canHarvest) {
            damage /= 30;
        }
        else {
            damage /= 100;
        }
        if (damage > 1)
            return 0;
        double ticks = Math.ceil(1 / damage);
        return ticks / 20;
    }

    private void initProcessMap() {
        processMap = new LinkedHashMap<>();
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
            if (!(recipeType == RecipeType.CRAFTING || recipeType == RecipeType.SMELTING || recipeType == RecipeType.SMITHING))
                continue;
            Identifier itemId = Registry.ITEM.getId(recipe.getOutput().getItem());
            if (!processMap.containsKey(itemId))
                processMap.put(itemId, new ArrayList<>());
            processMap.get(itemId).add(getCraftingProcessByType(recipe, recipeType));
        }
        LootManager manager = makeLootManager();
        for (Identifier id : Registry.BLOCK.getIds()) {
            List<Item> items;
            Block block = Registry.BLOCK.get(id);
            Identifier lootTableLocation = block.getLootTableId();
            if (lootTableLocation == LootTables.EMPTY) {
                items = Collections.emptyList();
            }
            else {
                items = new ArrayList<>();
                manager.getTable(lootTableLocation).generateLoot(
                        new LootContext.Builder((ServerWorld) null)
                                .random(new Random())
                                .parameter(LootContextParameters.ORIGIN, Vec3d.of(BlockPos.ORIGIN))
                                .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
                                .optionalParameter(LootContextParameters.BLOCK_ENTITY, null)
                                .parameter(LootContextParameters.BLOCK_STATE, block.getDefaultState())
                                .build(LootContextTypes.BLOCK),
                        stack -> items.add(stack.getItem())
                );
            }
            for (Item dropped : items) {
                Identifier itemId = Registry.ITEM.getId(dropped);
                if (!processMap.containsKey(itemId))
                    processMap.put(itemId, new ArrayList<>());
                processMap.get(itemId).add(new WorldCraftingProcess(dropped, block));
            }
        }
    }

    public class ToolCraftingManager {
        private LinkedHashSet<Item> tools;
        private LinkedHashSet<List<Item>> toolClasses;
        private LinkedHashSet<Item> essentialTools;
        public ToolCraftingManager() {
            tools = new LinkedHashSet<>();
            toolClasses = new LinkedHashSet<>();
            essentialTools = new LinkedHashSet<>();
            String[] prefixes = new String[] {"wooden", "stone", "iron", "golden", "diamond", "netherite"};
            String[] suffixes = new String[] {"sword", "axe", "pickaxe", "shovel", "hoe"};
            for (String suffix : suffixes) {
                List<Item> toolClass = new ArrayList<>();
                for (String prefix : prefixes) {
                    Item tool = Registry.ITEM.get(new Identifier("minecraft", prefix + "_" + suffix));
                    tools.add(tool);
                    if (suffix.equals("pickaxe") || suffix.equals("sword"))
                        essentialTools.add(tool);
                    toolClass.add(tool);
                }
                toolClasses.add(toolClass);
            }
            //tools.add(Registry.ITEM.get(new Identifier("minecraft", "shears")));
        }
        public List<Item> getClass(Item item) {
            for (List<Item> toolClass : toolClasses) {
                if (toolClass.contains(item))
                    return toolClass;
            }
            return new ArrayList<>();
        }
        public boolean isEssentialTool(Item item) {
            return essentialTools.contains(item);
        }
        public Set<Item> getInferiorToolsInClass(Item item) {
            List<Item> toolClass = getClass(item);
            int index = toolClass.indexOf(item);
            LinkedHashSet<Item> res = new LinkedHashSet<>();
            for (int i = index - 1; i >= 0; i--)
                res.add(toolClass.get(i));
            return res;
        }
        public Set<Item> getInferiorTools(Item item) {
            int index = getClass(item).indexOf(item);
            LinkedHashSet<Item> res = new LinkedHashSet<>();
            for (List<Item> toolClass : toolClasses) {
                for (int i = index - 1; i >= 0; i--)
                    res.add(toolClass.get(i));
            }
            return res;
        }
        public Set<Item> getMatchingTools(Block block) {
            Set<Item> res = new LinkedHashSet<>();
            BlockState state = block.getDefaultState();
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

    public class BlockManager {
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
            BlockUtils.rightClickBlockLegit(pos.down(), false, true, 5);
        }
    }

    private CraftingPlan getCraftingPlan(String name) {
        if (groups.containsKey(name)) {
            List<Item> items = groups.get(name).stream().toList();
            List<CraftingProcess> processes = List.of(new GroupCraftingProcess(items));
            return new CraftingPlan(items.get(0), processes);
        }
        Identifier id = new Identifier("minecraft", name);
        List<CraftingProcess> processes = new ArrayList<>(processMap.getOrDefault(id, new ArrayList<>()));
        processes.add(new InventoryCraftingProcess(Registry.ITEM.get(id)));
        processes.add(new StorageCraftingProcess(Registry.ITEM.get(id)));
        return new CraftingPlan(Registry.ITEM.get(id), processes);
    }

    public class SlotUpdateInfo {
        public int slot;
        public ItemStack itemStack;
        public SlotUpdateInfo(int slot, ItemStack itemStack) {
            this.slot = slot;
            this.itemStack = itemStack;
        }
    }

    public abstract class StorageQuery<T> {
        public abstract LinkedHashMap<T, Integer> getAvailabilityMap();
        public abstract boolean acquire(T item, Item dropped, int count);
    }

    public class InventoryStorageQuery extends StorageQuery<Item> {
        public InventoryStorageQuery() { }
        @Override
        public LinkedHashMap<Item, Integer> getAvailabilityMap() {
            LinkedHashMap<Item, Integer> res = new LinkedHashMap<>();
            List<ItemStack> items = MC.player.getInventory().main;
            for (ItemStack cur : items) {
                if (cur.getCount() > 0)
                    res.put(cur.getItem(), res.getOrDefault(cur.getItem(), 0) + cur.getCount());
            }
            return res;
        }
        public LinkedHashMap<Item, Integer> getScreenAvailabilityMap() {
            LinkedHashMap<Item, Integer> res = new LinkedHashMap<>();
            for (int i = 0; i < 36; i++) {
                Slot currentSlot = MC.player.playerScreenHandler.slots.get(i + 9);
                ItemStack stack = currentSlot.getStack();
                if (stack.getCount() > 0)
                    res.put(stack.getItem(), res.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
            return res;
        }
        public LinkedHashMap<Item, Integer> getCurrentContainerAvailabilityMap() {
            LinkedHashMap<Item, Integer> res = new LinkedHashMap<>();
            List<Slot> slots = MC.player.currentScreenHandler.slots;
            for (int i = 0; i < slots.size() - 36; i++) {
                if (slots.get(i).getStack().getCount() > 0)
                    res.put(slots.get(i).getStack().getItem(), res.getOrDefault(slots.get(i).getStack().getItem(), 0) + slots.get(i).getStack().getCount());
            }
            return res;
        }
        @Override
        public boolean acquire(Item item, Item dropped, int count) { return true; }
    }

    public abstract class Pathfinder {
        private boolean supportsMining;
        public Pathfinder(boolean supportsMining) {
            this.supportsMining = supportsMining;
        }
        public boolean isMiningSupported() {
            return supportsMining;
        }
        public abstract boolean path(BlockPos pos);
        public abstract boolean mine(Block block, Item dropped, int count);
    }

    public class WurstPathfinder extends Pathfinder {
        FlightHack flight;
        public WurstPathfinder() {
            super(false);
            flight = WurstClient.INSTANCE.getHax().flightHack;
        }
        public void setFlight(boolean status) {
            new WurstRunnable() {
                @Override
                protected void runInternal() {
                    flight.setEnabled(status);
                }
            }.runUntilDone();
        }
        public boolean path(BlockPos pos) {
            GoToCmd path = new GoToCmd();
            path.setGoal(pos);
            path.enable();
            return path.waitUntilDone();
        }
        public boolean mine(Block block, Item dropped, int count) {
            throw new NotImplementedException("Wurst pathfinder does not support mining");
        }
    }

    public abstract class WurstRunnable implements Runnable {
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
            runnableQueue.add(this);
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
        protected abstract void runInternal();
    }

    public abstract class MCRunnable implements Runnable {
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

    public abstract class BaritoneChatProcess {
        protected abstract void waitUntilDone();
    }

    public class BaritoneChatPathProcess extends BaritoneChatProcess {
        private BlockPos pos;
        public BaritoneChatPathProcess(BlockPos pos) {
            this.pos = pos;
        }
        @Override
        protected void waitUntilDone() {
            double epsilon = 1E-6;
            Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d lastPos = null;
            Vec3d currentPos = MC.player.getPos();
            double distance;
            try {
                while ((distance = blockPos.subtract(MC.player.getPos()).length()) > 1 && (lastPos == null || distance > 2.5 || currentPos.subtract(lastPos).length() > epsilon)) {
                    Thread.sleep(100);
                    lastPos = currentPos;
                    currentPos = MC.player.getPos();
                }
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class BaritoneChatMineProcess extends BaritoneChatProcess {
        private int count;
        private Item dropped;
        public BaritoneChatMineProcess(int count, Item dropped) {
            this.count = count;
            this.dropped = dropped;
        }
        @Override
        protected void waitUntilDone() {
            slotUpdateLock.lock();
            try {
                while (totalInventoryAvailabilityMap.getOrDefault(dropped, 0) < count) {
                    slotUpdateCondition.await(100, TimeUnit.MILLISECONDS);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                slotUpdateLock.unlock();
            }
        }
    }

    public class BaritoneChatInterface {
        public BaritoneChatInterface() { }
        private void sendCommand(String cmd) {
            new MCRunnable() {
                @Override
                protected void runInternal() {
                    MC.player.sendChatMessage("#" + cmd);
                }
            }.runUntilDone();
        }
        private void setSetting(String setting, String value) {
            sendCommand(setting + " " + value);
        }
        private void setAllowInventory(boolean value) {
            setSetting("allowInventory", value + "");
        }
        private void setAcceptableThrowawayItems(List<Item> items) {
            String settingValue;
            if (items.isEmpty()) {
                settingValue = ",";
            }
            else {
                settingValue = items.stream().map(item -> Registry.ITEM.getId(item).getPath()).collect(Collectors.joining(","));
            }
            setSetting("acceptableThrowawayItems", settingValue);
        }
        public BaritoneChatProcess path(BlockPos pos) {
            sendCommand("goto " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            return new BaritoneChatPathProcess(pos);
        }
        public BaritoneChatProcess mine(int count, Block block, Item dropped) {
            sendCommand("mine " + count + " " + Registry.BLOCK.getId(block).getPath());
            return new BaritoneChatMineProcess(count, dropped);
        }
    }

    public class BaritoneAPIInterface {
        private Class BaritoneAPI;
        private Class GoalBlock;
        private Class Goal;
        public BaritoneAPIInterface() {
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

    public class BaritoneChatPathfinder extends Pathfinder {
        private boolean updatedSettings = false;
        public BaritoneChatPathfinder() {
            super(true);
        }
        private void updateSettings() {
            if (!updatedSettings) {
                baritoneChatInterface.setAllowInventory(true);
                updatedSettings = true;
            }
        }
        public boolean path(BlockPos pos) {
            updateSettings();
            BaritoneChatProcess process = baritoneChatInterface.path(pos);
            process.waitUntilDone();
            return true;
        }
        public boolean mine(Block block, Item dropped, int count) {
            final int alreadyPossessed;
            if (isCurrentlyCrafting) {
                synchronized (totalInventoryAvailabilityMap) {
                    alreadyPossessed = totalInventoryAvailabilityMap.getOrDefault(dropped, 0);
                }
            }
            else {
                alreadyPossessed = inventoryQuery.getAvailabilityMap().getOrDefault(dropped, 0);
            }
            updateSettings();
            BaritoneChatProcess process = baritoneChatInterface.mine(count + alreadyPossessed, block, dropped);
            process.waitUntilDone();
            return true;
        }
    }

    /*public class BaritoneAPIPathfinder extends Pathfinder {
        public BaritoneAPIPathfinder() {
            super(true);
        }
        public boolean path(BlockPos pos) {
            Object pathProcess = baritoneAPIInterface.getCustomGoalProcess();
            baritoneAPIInterface.setAllowInventory(true);
            baritoneAPIInterface.setAcceptableThrowawayItems(new ArrayList<>());
            NotifyingRunnable baritoneRunnable = new NotifyingRunnable() {
                protected void runInternal() {
                    baritoneAPIInterface.setGoalAndPath(pathProcess, pos);
                }
            };
            baritoneRunnable.runUntilDone();
            while (baritoneAPIInterface.isActive(pathProcess)) {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            baritoneAPIInterface.setAllowInventory(false);
            return true;
        }
        public boolean mine(Block block, Item dropped, int count) {
            updateTotalInventoryAvailability();
            Object mineProcess = baritoneAPIInterface.getMineProcess();
            final int alreadyPossessed;
            if (isCurrentlyCrafting) {
                synchronized (totalInventoryAvailabilityMap) {
                    alreadyPossessed = totalInventoryAvailabilityMap.getOrDefault(block.asItem(), 0);
                }
            }
            else {
                alreadyPossessed = inventoryQuery.getAvailabilityMap().getOrDefault(block.asItem(), 0);
            }
            baritoneAPIInterface.setAllowInventory(true);
            baritoneAPIInterface.setAcceptableThrowawayItems(new ArrayList<>());
            NotifyingRunnable baritoneRunnable = new NotifyingRunnable() {
                protected void runInternal() {
                    baritoneAPIInterface.mine(mineProcess, count + alreadyPossessed, block);
                }
            };
            baritoneRunnable.runUntilDone();
            while (baritoneAPIInterface.isActive(mineProcess)) {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            baritoneAPIInterface.setAllowInventory(false);
            return true;
        }
    }*/

    public class ContainerStorageQuery extends StorageQuery<Item> {
        private final LinkedHashMap<BlockPos, LinkedHashMap<Item, Integer>> containers;
        public BlockPos nearestContainer = BlockPos.ORIGIN;
        public double nearestContainerDistance = 0.0;
        public ContainerStorageQuery() {
            containers = new LinkedHashMap<>();
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
        public void updateContainer(LinkedHashMap<Item, Integer> content, BlockPos pos) {
            synchronized (containers) {
                containers.put(pos, content);
            }
        }
        @Override
        public LinkedHashMap<Item, Integer> getAvailabilityMap() {
            LinkedHashMap<Item, Integer> globalAvailabilityMap = new LinkedHashMap<>();
            synchronized (containers) {
                for (LinkedHashMap<Item, Integer> map : containers.values()) {
                    for (Item item : map.keySet()) {
                        globalAvailabilityMap.put(item, globalAvailabilityMap.getOrDefault(item, 0) + map.get(item));
                    }
                }
            }
            return globalAvailabilityMap;
        }
        private int takeItem(BlockPos containerPos, Item item, int count) {
            int initialCount = count;
            LinkedHashMap<Item, Integer> container;
            synchronized (containers) {
                if (!containers.containsKey(containerPos))
                    return count;
                container = containers.get(containerPos);
            }
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
                            synchronized (totalInventoryAvailabilityMap) {
                                totalInventoryAvailabilityMap.put(item, totalInventoryAvailabilityMap.getOrDefault(item, 0) + count);
                            }
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
        public boolean acquire(Item item, Item dropped, int count) {
            Vec3d playerPos = MC.player.getPos();
            List<Pair<BlockPos, LinkedHashMap<Item, Integer>>> containerList;
            synchronized (containers) {
                containerList = containers.entrySet().stream().map(x -> new Pair<>(x.getKey(), x.getValue())).collect(Collectors.toList());
            }
            Function<BlockPos, Double> distanceFromPlayer = pos -> new Vec3d(pos.getX(), pos.getY(), pos.getZ()).subtract(playerPos).length();
            containerList.sort(Comparator.comparing(x -> distanceFromPlayer.apply(x.getLeft())));
            for (Pair<BlockPos, LinkedHashMap<Item, Integer>> pair : containerList) {
                BlockPos pos = pair.getLeft();
                if (count <= 0)
                    break;
                if (pair.getRight().getOrDefault(item, 0) > 0) {
                    containerManager.navigateAndOpenContainer(pos);
                    count = takeItem(pos, item, count);
                }
            }
            return count <= 0;
        }
        public double getDistanceToContainer(Item item) {
            Vec3d playerPos = MC.player.getPos();
            double closestDistance = Double.POSITIVE_INFINITY;
            synchronized (containers) {
                for (BlockPos pos : containers.keySet()) {
                    Vec3d containerPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                    LinkedHashMap<Item, Integer> container = containers.get(pos);
                    if (container.containsKey(item))
                        closestDistance = Math.min(closestDistance, containerPos.subtract(playerPos).length());
                }
            }
            return closestDistance;
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
        public void acquire(LinkedHashMap<Item, Integer> items) {
            for (BlockPos pos : containers.keySet()) {
                if (items.size() == 0)
                    break;
                LinkedHashMap<Item, Integer> container = containers.get(pos);
                Set<Item> itemKeySet = new LinkedHashSet<>(items.keySet());
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

    public class WorldStorageQuery extends StorageQuery<Block> {
        public WorldStorageQuery() { }
        public List<Chunk> getChunks(int range) {
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
        public LinkedHashMap<Block, Integer> getAvailabilityMap() {
            LinkedHashMap<Block, Integer> availability = new LinkedHashMap<>();
            applyToBlocks((b) -> { availability.put(b.getRight().getBlock(), availability.getOrDefault(b.getRight().getBlock(), 0) + 1); });
            return availability;
        }
        public LinkedHashMap<Block, BlockPos> getNearestPositions() {
            LinkedHashMap<Block, BlockPos> res = new LinkedHashMap<>();
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
        public LinkedHashMap<Block, Double> getNearestBlockDistances(LinkedHashMap<Block, BlockPos> nearestPositions) {
            LinkedHashMap<Block, Double> res = new LinkedHashMap<>();
            Vec3d playerPos = MC.player.getPos();
            for (Block block : nearestPositions.keySet()) {
                BlockPos pos = nearestPositions.get(block);
                Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
                res.put(block, playerPos.subtract(blockPos).length());
            }
            return res;
        }
        public LinkedHashMap<Block, LinkedHashSet<BlockPos>> getLocations(LinkedHashSet<Block> blocks) {
            LinkedHashMap<Block, LinkedHashSet<BlockPos>> res = new LinkedHashMap<>();
            applyToBlocks((b) -> {
                if (!blocks.contains(b.getRight().getBlock()))
                    return;
                LinkedHashSet<BlockPos> s = res.getOrDefault(b.getRight().getBlock(), new LinkedHashSet<>());
                s.add(b.getLeft());
                res.put(b.getRight().getBlock(), s);
            });
            return res;
        }
        @Override
        public boolean acquire(Block block, Item dropped, int count) {
            return getActivePathfinder().mine(block, dropped, count);
        }
    }

    public class CraftingPlan {
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
                return new ChoiceNode(target, processes);
            return processes.get(0).getNode();
        }
    }

    public abstract class CraftingProcess {
        public abstract int getMultiplicity();
        public abstract Node getNode();
    }

    public class RecipeCraftingProcess extends CraftingProcess {
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
            return new RecipeNode(recipe.getOutput().getItem(), List.of(this));
        }
    }

    public class SmithingCraftingProcess extends CraftingProcess {
        private Recipe<?> recipe;
        public SmithingCraftingProcess(Recipe<?> recipe) {
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
            return new SmithingNode(recipe.getOutput().getItem(), List.of(this));
        }
    }

    public class SmeltingCraftingProcess extends CraftingProcess {
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
            return new SmeltingNode(recipe.getOutput().getItem(), List.of(this));
        }
    }

    public class InventoryCraftingProcess extends CraftingProcess {
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
            return new InventoryNode(item, List.of(this));
        }
    }

    public class StorageCraftingProcess extends CraftingProcess {
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
            return new StorageNode(item, List.of(this));
        }
    }

    public class WorldCraftingProcess extends CraftingProcess {
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
            return new WorldNode(dropped, block, List.of(this));
        }
    }

    public class PathingCraftingProcess extends CraftingProcess {
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
            return new PathingNode(block.asItem(), List.of(this));
        }
    }

    public class PlacementCraftingProcess extends CraftingProcess {
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
            return new PlacementNode(block.asItem(), List.of(this));
        }
    }

    public class WorkbenchCraftingProcess extends CraftingProcess {
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
            return new WorkbenchNode(block.asItem(), List.of(this));
        }
    }

    public class ToolCraftingProcess extends CraftingProcess {
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
            return new ToolNode(block.asItem(), List.of(this));
        }
    }

    public class GroupCraftingProcess extends CraftingProcess {
        private List<Item> items;
        public GroupCraftingProcess(List<Item> items) {
            this.items = items;
        }
        @Override
        public int getMultiplicity() {
            return 1;
        }
        @Override
        public Node getNode() {
            return new GroupNode(items.get(0), List.of(this));
        }
    }

    private boolean pathTo(BlockPos pos) {
        if (flightPathsEnabled.isChecked()) {
            wurstPathfinder.setFlight(true);
            boolean success = wurstPathfinder.path(pos);
            wurstPathfinder.setFlight(false);
            if (success)
                return true;
        }
        return getActivePathfinder().path(pos);
    }

    public class ContainerManager {
        private LinkedHashMap<Block, LinkedHashSet<BlockPos>> containers = new LinkedHashMap<>();
        private BlockPos currentContainer = null;
        private BlockPos getNearestPathablePosition(BlockPos containerPos) {
            int range = 4;
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
            MCRunnable inventoryRunnable = new MCRunnable() {
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
            return pathTo(nearestPathablePosition);
        }
        public boolean navigateAndOpenContainer(BlockPos container) {
            if (!navigateToContainer(container))
                return false;
            BlockUtils.rightClickBlockLegit(container, false, false, 5);
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
            LinkedHashSet<BlockPos> positions = containers.getOrDefault(block, new LinkedHashSet<>());
            positions.add(pos);
            containers.put(block, positions);
        }
        public void updateContainers(LinkedHashMap<Block, LinkedHashSet<BlockPos>> updated) {
            for (Block b : updated.keySet()) {
                LinkedHashSet<BlockPos> positions = containers.getOrDefault(b, new LinkedHashSet<>());
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
        public LinkedHashMap<Block, LinkedHashSet<BlockPos>> getContainers() {
            return containers;
        }
    }

    public class InventoryManager {
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

    private boolean awaitSlotUpdate(Item item, int amount, int slot, boolean onlyConsiderItem, boolean succeedAfterTimeout, boolean pollContinuously) {
        slotUpdateLock.lock();
        initSlotUpdates();
        try {
            while (!latestSlotUpdates.containsKey(slot) || !latestSlotUpdates.get(slot).itemStack.getItem().equals(item) || (!onlyConsiderItem && latestSlotUpdates.get(slot).itemStack.getCount() != amount)) {
                boolean gotSignal = slotUpdateCondition.await(1000, TimeUnit.MILLISECONDS);
                if (succeedAfterTimeout)
                    return true;
                if (!gotSignal) {
                    ItemStack craftingItem = MC.player.currentScreenHandler.getSlot(slot).getStack();
                    if (craftingItem.getItem().equals(item) && (onlyConsiderItem || (craftingItem.getCount() == amount && slot == 0)))
                        return true;
                    if (pollContinuously)
                        continue;
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

    public class NodeGraphGenerationInfo {
        private LinkedHashMap<Node, LinkedHashSet<Node>> backlog;
        private List<NodeEquivalenceClass> newEquivalenceClasses;
        private List<NodeEquivalenceClass> newProfiles;
        public boolean success;
        public NodeGraphGenerationInfo() {
            backlog = new LinkedHashMap<>();
            newEquivalenceClasses = new ArrayList<>();
            newProfiles = new ArrayList<>();
        }
        public NodeGraphGenerationInfo(LinkedHashMap<Node, LinkedHashSet<Node>> backlog, List<NodeEquivalenceClass> newEquivalenceClasses, List<NodeEquivalenceClass> newProfiles) {
            this.backlog = backlog;
            this.newEquivalenceClasses = newEquivalenceClasses;
            this.newProfiles = newProfiles;
        }
        public NodeGraphGenerationInfo success() {
            success = true;
            return this;
        }
        public NodeGraphGenerationInfo failure() {
            success = false;
            return this;
        }
    }

    public class CraftingState {
        private LinkedHashMap<Item, Integer> inventoryAvailability;
        private LinkedHashMap<Item, Integer> storageAvailability;
        private LinkedHashMap<Block, Integer> worldAvailability;
        private LinkedHashMap<Long, Double> efficiencyMap = new LinkedHashMap<>();
        private LinkedHashMap<Node, Double> naiveEfficiencyMap = new LinkedHashMap<>();
        private LinkedHashMap<Node, Boolean> naiveEfficiencyMapGenerated = new LinkedHashMap<>();
        private LinkedHashSet<Item> visited;
        private LinkedHashSet<Item> resetVisited = new LinkedHashSet<>();
        private List<StorageNode> storageNodes = new ArrayList<>();
        private LinkedHashMap<Item, Integer> craftingItemFrequency = new LinkedHashMap<>();
        private LinkedHashSet<Long> deadNodes = new LinkedHashSet<>();
        private LinkedHashMap<Node, List<Long>> children = new LinkedHashMap<>();
        private LinkedHashMap<Long, Integer> neededMap = new LinkedHashMap<>();
        private LinkedHashMap<Node, Integer> naiveMaxCraftable = new LinkedHashMap<>();
        private LinkedHashMap<Node, Boolean> naiveMaxCraftableGenerated = new LinkedHashMap<>();
        private LinkedHashMap<Long, Integer> timeTaken = new LinkedHashMap<>();
        private LinkedHashMap<Item, List<Pair<Long, Pair<Node, Integer>>>> excess = new LinkedHashMap<>();
        private LinkedHashMap<Long, LinkedHashMap<Long, Pair<Node, Integer>>> excessConsumed = new LinkedHashMap<>();
        private LinkedHashMap<Long, LinkedHashMap<Long, Pair<Node, Integer>>> excessProduced = new LinkedHashMap<>();
        private LinkedHashMap<Long, Integer> resourceConsumed = new LinkedHashMap<>();
        private LinkedHashMap<Long, Integer> inventoryConsumed = new LinkedHashMap<>();
        private LinkedHashMap<Long, Integer> storageConsumed = new LinkedHashMap<>();
        private LinkedHashMap<Long, Integer> worldConsumed = new LinkedHashMap<>();
        private LinkedHashMap<NodeEquivalenceClass, NodeEquivalenceClass> equivalenceRelation = new LinkedHashMap<>();
        private LinkedHashMap<Node, Node> nodeInstances = new LinkedHashMap<>();
        private LinkedHashMap<NodeEquivalenceClass, LinkedHashSet<Item>> instanceProfile = new LinkedHashMap<>();
        private LinkedHashMap<Long, Node> idInstanceMap = new LinkedHashMap<>();
        private LinkedHashMap<Node, Long> childScope = new LinkedHashMap<>();
        private LinkedHashSet<Item> throwawayItems = new LinkedHashSet<>();
        private LinkedHashMap<Node, Resources<OperableInteger>> cachedResources = new LinkedHashMap<>();
        private LinkedHashMap<Node, Double> cachedEfficiency = new LinkedHashMap<>();
        private LinkedHashMap<Node, EfficiencyEquation> efficiencyEquations = new LinkedHashMap<>();
        private LinkedHashMap<Node, Boolean> efficiencyEquationsGenerated = new LinkedHashMap<>();
        private LinkedHashSet<Long> excessBlacklist = new LinkedHashSet<>();
        private Stack<LinkedHashSet<Item>> visitedStack = new Stack<>();
        private int rootNodeId = 0;
        private static int maxVisitedSize = 0;
        private boolean success = false;
        public CraftingState(LinkedHashMap<Item, Integer> inventoryAvailability, LinkedHashMap<Item, Integer> storageAvailability, LinkedHashMap<Block, Integer> worldAvailability, LinkedHashSet<Item> visited) {
            this.inventoryAvailability = inventoryAvailability;
            this.storageAvailability = storageAvailability;
            this.worldAvailability = worldAvailability;
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
        private LinkedHashMap<Item, List<Pair<Long, Pair<Node, Integer>>>> deepcopyExcess(LinkedHashMap<Item, List<Pair<Long, Pair<Node, Integer>>>> excess) {
            LinkedHashMap<Item, List<Pair<Long, Pair<Node, Integer>>>> res = new LinkedHashMap<>();
            for (Item item : excess.keySet()) {
                List<Pair<Long, Pair<Node, Integer>>> value = excess.get(item);
                List<Pair<Long, Pair<Node, Integer>>> copy = new ArrayList<>();
                for (Pair<Long, Pair<Node, Integer>> instance : value) {
                    copy.add(new Pair<>(instance.getLeft(), new Pair<>(instance.getRight().getLeft(), instance.getRight().getRight())));
                }
                res.put(item, copy);
            }
            return res;
        }
        private LinkedHashMap<Long, LinkedHashMap<Long, Pair<Node, Integer>>> deepcopyExcessInfo(LinkedHashMap<Long, LinkedHashMap<Long, Pair<Node, Integer>>> info) {
            LinkedHashMap<Long, LinkedHashMap<Long, Pair<Node, Integer>>> res = new LinkedHashMap<>();
            for (Long key : info.keySet()) {
                LinkedHashMap<Long, Pair<Node, Integer>> value = info.get(key);
                LinkedHashMap<Long, Pair<Node, Integer>> copy = new LinkedHashMap<>();
                for (Long valueKey : value.keySet()) {
                    Pair<Node, Integer> valueValue = value.get(valueKey);
                    copy.put(valueKey, new Pair<>(valueValue.getLeft(), valueValue.getRight()));
                }
                res.put(key, copy);
            }
            return res;
        }
        private Stack<LinkedHashSet<Item>> deepcopyVisitedStack(Stack<LinkedHashSet<Item>> stack) {
            Stack<LinkedHashSet<Item>> res = new Stack<>();
            for (LinkedHashSet<Item> item : stack) {
                res.add((LinkedHashSet<Item>) item.clone());
            }
            return res;
        }
        public CraftingState clone() {
            CraftingState state = new CraftingState((LinkedHashMap<Item, Integer>) inventoryAvailability.clone(), (LinkedHashMap<Item, Integer>) storageAvailability.clone(), (LinkedHashMap<Block, Integer>) worldAvailability.clone(), (LinkedHashSet<Item>) visited.clone());
            state.resetVisited = (LinkedHashSet<Item>) resetVisited.clone();
            state.success = success;
            state.storageNodes = new ArrayList<>(storageNodes);
            state.craftingItemFrequency = (LinkedHashMap<Item, Integer>) craftingItemFrequency.clone();
            state.efficiencyMap = (LinkedHashMap<Long, Double>) efficiencyMap.clone();
            state.naiveEfficiencyMap = naiveEfficiencyMap;
            state.naiveEfficiencyMapGenerated = naiveEfficiencyMapGenerated;
            state.deadNodes = (LinkedHashSet<Long>) deadNodes.clone();
            state.children = children;
            state.neededMap = (LinkedHashMap<Long, Integer>) neededMap.clone();
            state.naiveMaxCraftable = naiveMaxCraftable;
            state.naiveMaxCraftableGenerated = naiveMaxCraftableGenerated;
            state.timeTaken = timeTaken;
            state.excess = deepcopyExcess(excess);
            state.excessConsumed = deepcopyExcessInfo(excessConsumed);
            state.excessProduced = deepcopyExcessInfo(excessProduced);
            state.resourceConsumed = (LinkedHashMap<Long, Integer>) resourceConsumed.clone();
            state.inventoryConsumed = (LinkedHashMap<Long, Integer>) inventoryConsumed.clone();
            state.storageConsumed = (LinkedHashMap<Long, Integer>) storageConsumed.clone();
            state.worldConsumed = (LinkedHashMap<Long, Integer>) worldConsumed.clone();
            state.nodeInstances = nodeInstances;
            state.rootNodeId = rootNodeId;
            state.idInstanceMap = idInstanceMap;
            state.childScope = childScope;
            state.throwawayItems = throwawayItems;
            state.cachedResources = cachedResources;
            state.cachedEfficiency = cachedEfficiency;
            state.efficiencyEquations = efficiencyEquations;
            state.efficiencyEquationsGenerated = efficiencyEquationsGenerated;
            state.excessBlacklist = (LinkedHashSet<Long>) excessBlacklist.clone();
            state.visitedStack = deepcopyVisitedStack(visitedStack);
            return state;
        }
        public void set(CraftingState other) {
            inventoryAvailability = other.inventoryAvailability;
            storageAvailability = other.storageAvailability;
            worldAvailability = other.worldAvailability;
            visited = other.visited;
            resetVisited = other.resetVisited;
            success = other.success;
            storageNodes = other.storageNodes;
            craftingItemFrequency = other.craftingItemFrequency;
            efficiencyMap = other.efficiencyMap;
            naiveEfficiencyMap = other.naiveEfficiencyMap;
            naiveEfficiencyMapGenerated = other.naiveEfficiencyMapGenerated;
            deadNodes = other.deadNodes;
            children = other.children;
            neededMap = other.neededMap;
            naiveMaxCraftable = other.naiveMaxCraftable;
            naiveMaxCraftableGenerated = other.naiveMaxCraftableGenerated;
            timeTaken = other.timeTaken;
            excess = other.excess;
            excessConsumed = other.excessConsumed;
            excessProduced =  other.excessProduced;
            resourceConsumed = other.resourceConsumed;
            inventoryConsumed = other.inventoryConsumed;
            storageConsumed = other.storageConsumed;
            worldConsumed = other.worldConsumed;
            nodeInstances = other.nodeInstances;
            rootNodeId = other.rootNodeId;
            idInstanceMap = other.idInstanceMap;
            childScope = other.childScope;
            throwawayItems = other.throwawayItems;
            cachedResources = other.cachedResources;
            cachedEfficiency = other.cachedEfficiency;
            efficiencyEquations = other.efficiencyEquations;
            efficiencyEquationsGenerated = other.efficiencyEquationsGenerated;
            excessBlacklist = other.excessBlacklist;
            visitedStack = other.visitedStack;
        }
        private void generateThrowawayItems(NodeEquivalenceClass node) {
            List<String> itemNames = List.of("dirt", "cobblestone", "stone", "netherrack", "granite", "diorite", "andesite", "gravel", "sand", "end_stone");
            LinkedHashSet<Item> profile = instanceProfile.get(node);
            throwawayItems = itemNames.stream().map(name ->  Registry.ITEM.get(new Identifier("minecraft", name))).filter(item -> !profile.contains(item)).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        public boolean generateTree(long id, Node node) {
            generateNodeInstanceGraph(node, new LinkedHashMap<>());
            if (!equivalenceRelation.containsKey(new NodeEquivalenceClass(node)))
                return false;
            generateInstanceProfiles(new NodeEquivalenceClass(node));
            generateThrowawayItems(new NodeEquivalenceClass(node));
            generateTreeInternal(id, node);
            node.genNaiveMaxCraftable(this);
            return true;
        }
        private void clearInstanceGraph(Node node) {
            equivalenceRelation.remove(new NodeEquivalenceClass(node));
            instanceProfile.remove(new NodeEquivalenceClass(node));
        }
        private void populateIntegerGraph(NodeEquivalenceClass node, AtomicInteger id, LinkedHashMap<Integer, NodeEquivalenceClass> forwardMapping, LinkedHashMap<NodeEquivalenceClass, Integer> reverseMapping, LinkedHashMap<Integer, List<Integer>> graph) {
            int nodeId = id.get();
            forwardMapping.put(nodeId, node);
            reverseMapping.put(node, nodeId);
            List<Integer> outboundEdges = new ArrayList<>();
            graph.put(nodeId, outboundEdges);
            for (Node child : node.node.children) {
                NodeEquivalenceClass childEquivalenceClass = new NodeEquivalenceClass(child);
                if (reverseMapping.containsKey(childEquivalenceClass)) {
                    outboundEdges.add(reverseMapping.get(childEquivalenceClass));
                }
                else {
                    id.set(id.get() + 1);
                    outboundEdges.add(id.get());
                    populateIntegerGraph(childEquivalenceClass, id, forwardMapping, reverseMapping, graph);
                }
            }
        }
        private Pair<LinkedHashMap<Integer, NodeEquivalenceClass>, LinkedHashMap<Integer, List<Integer>>> convertToIntegerGraph(NodeEquivalenceClass rootNode, int rootId) {
            LinkedHashMap<Integer, NodeEquivalenceClass> forwardMapping = new LinkedHashMap<>();
            LinkedHashMap<NodeEquivalenceClass, Integer> reverseMapping = new LinkedHashMap<>();
            LinkedHashMap<Integer, List<Integer>> integerGraph = new LinkedHashMap<>();
            populateIntegerGraph(rootNode, new AtomicInteger(rootId), forwardMapping, reverseMapping, integerGraph);
            return new Pair<>(forwardMapping, integerGraph);
        }
        private LinkedHashMap<Integer, List<Integer>> graphTranspose(LinkedHashMap<Integer, List<Integer>> graph) {
            LinkedHashMap<Integer, List<Integer>> res = new LinkedHashMap<>();
            for (Integer node : graph.keySet()) {
                List<Integer> nodeChildren = graph.get(node);
                for (Integer child : nodeChildren) {
                    if (!res.containsKey(child))
                        res.put(child, new ArrayList<>());
                    res.get(child).add(node);
                }
            }
            return res;
        }
        private void graphDFS(Stack<Integer> stack, LinkedHashSet<Integer> visited, LinkedHashMap<Integer, List<Integer>> graph, int id) {
            visited.add(id);
            List<Integer> nodeChildren = graph.get(id);
            for (int child : nodeChildren) {
                if (!visited.contains(child))
                    graphDFS(stack, visited, graph, child);
            }
            stack.push(id);
        }
        private void transposeGraphDFS(LinkedHashSet<Integer> component, LinkedHashSet<Integer> visited, LinkedHashSet<Integer> totalNodes, LinkedHashMap<Integer, List<Integer>> graph, int id) {
            visited.add(id);
            List<Integer> nodeChildren = graph.get(id);
            for (int child : nodeChildren) {
                if (!visited.contains(child) && !totalNodes.contains(child))
                    transposeGraphDFS(component, visited, totalNodes, graph, child);
            }
            component.add(id);
        }
        private List<LinkedHashSet<NodeEquivalenceClass>> findStronglyConnectedComponents(NodeEquivalenceClass rootNode) {
            int rootId = 0;
            Pair<LinkedHashMap<Integer, NodeEquivalenceClass>, LinkedHashMap<Integer, List<Integer>>> graphPair = convertToIntegerGraph(rootNode, rootId);
            LinkedHashMap<Integer, NodeEquivalenceClass> nodeMapping = graphPair.getLeft();
            LinkedHashMap<Integer, List<Integer>> graph = graphPair.getRight();
            LinkedHashMap<Integer, List<Integer>> transpose = graphTranspose(graph);
            Stack<Integer> nodeStack = new Stack<>();
            graphDFS(nodeStack, new LinkedHashSet<>(), graph, rootId);
            LinkedHashSet<Integer> totalNodes = new LinkedHashSet<>();
            List<LinkedHashSet<NodeEquivalenceClass>> res = new ArrayList<>();
            while (!nodeStack.empty()) {
                int id = nodeStack.pop();
                if (!transpose.containsKey(id))
                    transpose.put(id, new ArrayList<>());
                if (totalNodes.contains(id))
                    continue;
                LinkedHashSet<Integer> component = new LinkedHashSet<>();
                transposeGraphDFS(component, new LinkedHashSet<>(), totalNodes, transpose, id);
                totalNodes.addAll(component);
                res.add(component.stream().map(nodeMapping::get).collect(Collectors.toCollection(LinkedHashSet::new)));
            }
            return res;
        }
        private void calculateComponentProfile(List<LinkedHashSet<NodeEquivalenceClass>> components, LinkedHashMap<Integer, LinkedHashSet<Item>> componentProfiles, LinkedHashMap<Integer, LinkedHashSet<Integer>> componentGraph, int component) {
            LinkedHashSet<Item> res = components.get(component).stream().filter(node -> node.node.shouldPruneTarget).map(node -> node.node.target).collect(Collectors.toCollection(LinkedHashSet::new));
            if (componentGraph.containsKey(component)) {
                for (int childComponent : componentGraph.get(component)) {
                    if (!componentProfiles.containsKey(childComponent)) {
                        calculateComponentProfile(components, componentProfiles, componentGraph, childComponent);
                    }
                    res.addAll(componentProfiles.get(childComponent));
                }
            }
            componentProfiles.put(component, res);
        }
        private void generateInstanceProfiles(NodeEquivalenceClass rootNode) {
            List<LinkedHashSet<NodeEquivalenceClass>> components = findStronglyConnectedComponents(rootNode);
            LinkedHashMap<NodeEquivalenceClass, Integer> nodeComponentMap = new LinkedHashMap<>();
            for (int i = 0; i < components.size(); i++) {
                LinkedHashSet<NodeEquivalenceClass> component = components.get(i);
                for (NodeEquivalenceClass node : component) {
                    nodeComponentMap.put(node, i);
                }
            }
            LinkedHashMap<Integer, LinkedHashSet<Integer>> componentGraph = new LinkedHashMap<>();
            for (NodeEquivalenceClass node : nodeComponentMap.keySet()) {
                int component = nodeComponentMap.get(node);
                for (Node child : node.node.children) {
                    NodeEquivalenceClass childClass = new NodeEquivalenceClass(child);
                    int childComponent = nodeComponentMap.get(childClass);
                    if (component == childComponent)
                        continue;
                    if (!componentGraph.containsKey(component))
                        componentGraph.put(component, new LinkedHashSet<>());
                    componentGraph.get(component).add(childComponent);
                }
            }
            LinkedHashMap<Integer, LinkedHashSet<Item>> componentProfiles = new LinkedHashMap<>();
            for (int i = 0; i < components.size(); i++) {
                if (componentProfiles.containsKey(i))
                    continue;
                calculateComponentProfile(components, componentProfiles, componentGraph, i);
            }
            for (int i = 0; i < components.size(); i++) {
                LinkedHashSet<NodeEquivalenceClass> component = components.get(i);
                LinkedHashSet<Item> componentProfile = componentProfiles.get(i);
                for (NodeEquivalenceClass node : component) {
                    instanceProfile.put(node, componentProfile);
                }
            }
        }
        private NodeGraphGenerationInfo generateNodeInstanceGraph(Node node, LinkedHashMap<Node, Node> ancestors) {
            if (!node.canPossiblyCraft(this))
                return new NodeGraphGenerationInfo().failure();
            /*if (node.shouldPruneTarget && visited.contains(node.target))
                return;
            if (node.shouldRememberVisit())
                visited.add(node.target);*/
            assert !equivalenceRelation.containsKey(new NodeEquivalenceClass(node));
            ancestors.put(node, node);
            NodeEquivalenceClass equivalenceClass = new NodeEquivalenceClass(node);
            List<NodeEquivalenceClass> newEquivalenceClasses = new ArrayList<>();
            //List<NodeEquivalenceClass> newProfiles = new ArrayList<>();
            equivalenceRelation.put(equivalenceClass, equivalenceClass);
            newEquivalenceClasses.add(equivalenceClass);
            List<Node> nodeChildren = node.getChildren(this);
            node.children = nodeChildren;
            //LinkedHashSet<Item> profile = new LinkedHashSet<>();
            //if (node.shouldPruneTarget)
            //    profile.add(node.target);
            LinkedHashMap<Node, LinkedHashSet<Node>> res = new LinkedHashMap<>();
            boolean removedChildren = false;
            for (int i = nodeChildren.size() - 1; i >= 0; i--) {
                Node child = nodeChildren.get(i);
                if (ancestors.containsKey(child)) {
                    addToNodeBacklog(res, child, node);
                    nodeChildren.set(i, ancestors.get(child));
                }
                else if (equivalenceRelation.containsKey(new NodeEquivalenceClass(child))) {
                    //profile.addAll(instanceProfile.get(new NodeEquivalenceClass(child)));
                    nodeChildren.set(i, equivalenceRelation.get(new NodeEquivalenceClass(child)).node);
                }
                else {
                    NodeGraphGenerationInfo childRes = generateNodeInstanceGraph(child, ancestors);
                    if (!childRes.success) {
                        for (NodeEquivalenceClass n : childRes.newEquivalenceClasses) {
                            equivalenceRelation.remove(n);
                        }
                        //for (NodeEquivalenceClass n : childRes.newProfiles) {
                        //    instanceProfile.remove(n);
                        //}
                        if (node.requiresAllChildren()) {
                            ancestors.remove(node);
                            return new NodeGraphGenerationInfo(res, newEquivalenceClasses, null).failure();
                        }
                        else {
                            removedChildren = true;
                            nodeChildren.remove(i);
                        }
                    }
                    else {
                        newEquivalenceClasses.addAll(childRes.newEquivalenceClasses);
                        //newProfiles.addAll(childRes.newProfiles);
                        mergeNodeBacklogs(res, childRes.backlog);
                        //profile.addAll(instanceProfile.get(new NodeEquivalenceClass(child)));
                    }
                }
            }
            if (removedChildren && nodeChildren.size() == 0 && !node.requiresAllChildren() && node.requiresAnyChildren()) {
                clearInstanceGraph(node);
                ancestors.remove(node);
                return new NodeGraphGenerationInfo().failure();
            }
            if (res.containsKey(node)) {
                LinkedHashSet<Node> backloggedNodes = res.get(node);
                for (Node descendant : backloggedNodes) {
                    for (Node backloggedAncestor : res.keySet()) {
                        addToNodeBacklog(res, backloggedAncestor, descendant);
                    }
                    //instanceProfile.get(new NodeEquivalenceClass(descendant)).addAll(profile);
                }
                res.remove(node);
            }
            for (Node key : res.keySet()) {
                res.get(key).add(node);
            }
            NodeEquivalenceClass profileClass = new NodeEquivalenceClass(node);
            //instanceProfile.put(profileClass, profile);
            //newProfiles.add(profileClass);
            ancestors.remove(node);
            assert ancestors.size() != 0 || (res.size() == 0);
            return new NodeGraphGenerationInfo(res, newEquivalenceClasses, null).success();
        }
        private void addToNodeBacklog(LinkedHashMap<Node, LinkedHashSet<Node>> backlog, Node dest, Node src) {
            LinkedHashSet<Node> entry = backlog.getOrDefault(dest, new LinkedHashSet<>());
            entry.add(src);
            backlog.put(dest, entry);
        }
        private void mergeNodeBacklogs(LinkedHashMap<Node, LinkedHashSet<Node>> dest, LinkedHashMap<Node, LinkedHashSet<Node>> src) {
            for (Node item : src.keySet()) {
                if (!dest.containsKey(item)) {
                    dest.put(item, src.get(item));
                }
                else {
                    LinkedHashSet<Node> entry = dest.get(item);
                    entry.addAll(src.get(item));
                }
            }
        }
        private LinkedHashSet<Item> linkedHashSetOverlap(LinkedHashSet<Item> set1, LinkedHashSet<Item> set2) {
            LinkedHashSet<Item> result = new LinkedHashSet<>();
            LinkedHashSet<Item> smallest = set1.size() < set2.size() ? set1 : set2;
            LinkedHashSet<Item> biggest = smallest == set1 ? set2 : set1;
            for (Item item : smallest) {
                if (biggest.contains(item))
                    result.add(item);
            }
            return result;
        }
        public boolean addVisited(LinkedHashSet<Item> visited, LinkedHashSet<Item> resetVisited, Node node) {
            if (node.shouldPruneNow(visited, resetVisited))
                return false;
            if (node.shouldPruneTarget) {
                if (visited.contains(node.target))
                    return false;
                if (node.shouldResetVisited && resetVisited.contains(node.target))
                    return false;
            }
            if (node.shouldResetVisited)
                resetVisited.add(node.target);
            if (node.shouldRememberVisit())
                visited.add(node.target);
            return true;
        }
        public void removeVisited(LinkedHashSet<Item> visited, LinkedHashSet<Item> resetVisited, Node node) {
            if (node.shouldResetVisited)
                resetVisited.remove(node.target);
            if (node.shouldRememberVisit())
                visited.remove(node.target);
        }
        public void pushVisitedClone(Node node) {
            if (node.shouldResetVisited) {
                LinkedHashSet<Item> visitedClone = (LinkedHashSet<Item>) visited.clone();
                visitedStack.push(visitedClone);
                visited.clear();
            }
        }
        public void popVisitedClone(Node node) {
            if (node.shouldResetVisited) {
                visited = visitedStack.pop();
            }
        }
        private long generateTreeInternal(long id, Node node) {
            assert node != null;
            assert node.canPossiblyCraft(this);
            assert !nodeInstances.containsKey(node);
            assert equivalenceRelation.containsKey(new NodeEquivalenceClass(node));
            assert equivalenceRelation.get(new NodeEquivalenceClass(node)) != null;
            if (!addVisited(visited, resetVisited, node))
                return -1;
            List<Node> nodeChildren;
            if (nodeInstances.containsKey(node)) {
                node = nodeInstances.get(node);
                nodeChildren = node.children;
            }
            else {
                nodeChildren = equivalenceRelation.get(new NodeEquivalenceClass(node)).cloneChildren();
                node.children = nodeChildren;
                nodeInstances.put(node, node);
            }
            int initialChildren = nodeChildren.size();
            List<Long> nodeChildIds = children.getOrDefault(node, new ArrayList<>());
            children.put(node, nodeChildIds);
            for (int i = nodeChildren.size() - 1; i >= 0; i--) {
                Node child = nodeChildren.get(i);
                assert equivalenceRelation.containsKey(new NodeEquivalenceClass(child));
                assert instanceProfile.containsKey(new NodeEquivalenceClass(child));
                child.overlap = linkedHashSetOverlap(visited, instanceProfile.get(new NodeEquivalenceClass(child)));
                child.resetOverlap = linkedHashSetOverlap(resetVisited, instanceProfile.get(new NodeEquivalenceClass(child)));
                child.resetVisited = (LinkedHashSet<Item>) resetVisited.clone();
                nodeChildIds.add(0, id + 1);
                boolean pruneChild = child.shouldPruneTarget && visited.contains(child.target);
                if (nodeInstances.containsKey(child)) {
                    if (naiveMaxCraftable.getOrDefault(child, 0) > 0 || child.shouldAlwaysConsiderPossible()) {
                        Node match = nodeInstances.get(child);
                        nodeChildren.set(i, match);
                        child = match;
                        if (!pruneChild) {
                            id = id + childScope.getOrDefault(child, 0L) + 1;
                        }
                    }
                }
                else {
                    pushVisitedClone(child);
                    long retval = generateTreeInternal(0, child);
                    popVisitedClone(child);
                    if (retval != -1) {
                        id = id + retval + 1;
                        child.genNaiveMaxCraftable(this);
                    }
                }
                if (naiveMaxCraftable.getOrDefault(child, 0) == 0 && !child.shouldAlwaysConsiderPossible()) {
                    //clearChildren(child);
                    if (node.requiresAllChildren()) {
                        removeVisited(visited, resetVisited, node);
                        return -1;
                    }
                    else {
                        nodeChildren.remove(i);
                        nodeChildIds.remove(0);
                    }
                }
            }
            childScope.put(node, id);
            removeVisited(visited, resetVisited, node);
            if (initialChildren > 0 && nodeChildren.size() == 0 && node.requiresAnyChildren())
                return -1;
            return id;
        }
    }

    public class NodeEquivalenceClass {
        private Node node;
        public NodeEquivalenceClass(Node node) {
            this.node = node;
        }
        public List<Node> cloneChildren() {
            List<Node> res = new ArrayList<>();
            for (Node child : node.children) {
                res.add(child.clone());
            }
            return res;
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof NodeEquivalenceClass) {
                return node.equivalenceClassEquals(((NodeEquivalenceClass) other).node);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return node.equivalenceClassHashCode();
        }
    }

    public class EfficiencyEquationSegment {
        private double offset = 0.0;
        private double base;
        private double coefficient;
        private double start;
        private double end;
        public EfficiencyEquationSegment(double base, double coefficient, double start, double end) {
            this.base = base;
            this.coefficient = coefficient;
            this.start = start;
            this.end = end;
        }
        public EfficiencyEquationSegment setOffset(double offset) {
            this.offset = offset;
            return this;
        }
        public double evaluate(double num) {
            return offset + evaluateRelative(num);
        }
        public double evaluateRelative(double num) {
            return base + coefficient * (num - start);
        }
        public EfficiencyEquationSegment mult(double factor) {
            return new EfficiencyEquationSegment(base, coefficient * factor, start, start + (end - start) / factor).setOffset(offset);
        }
        public EfficiencyEquationSegment clone() {
            return new EfficiencyEquationSegment(base, coefficient, start, end).setOffset(offset);
        }
    }

    public class EfficiencyEquation {
        private List<EfficiencyEquationSegment> segments;
        public EfficiencyEquation(List<EfficiencyEquationSegment> components) {
            this.segments = components;
        }
        public EfficiencyEquation() {
            segments = new ArrayList<>();
        }
        public double getEfficiencyRatio() {
            if (segments.size() == 0)
                return Double.POSITIVE_INFINITY;
            EfficiencyEquationSegment lastSegment = segments.get(segments.size() - 1);
            return lastSegment.evaluate(lastSegment.end) / lastSegment.end;
        }
        public double singleProductionCost() {
            if (segments.size() == 0)
                return Double.POSITIVE_INFINITY;
            return evaluate(1);
        }
        public double evaluate(int num) {
            double epsilon = 1E-6;
            if (segments.size() == 0)
                return 0;
            for (EfficiencyEquationSegment segment : segments) {
                if (num >= segment.start - epsilon && num <= segment.end + epsilon)
                    return segment.evaluate(num);
            }
            return segments.get(segments.size() - 1).evaluate(num);
        }
        public EfficiencyEquation clamp(double factor) {
            List<EfficiencyEquationSegment> res = new ArrayList<>();
            for (EfficiencyEquationSegment segment : segments) {
                if (factor <= segment.end) {
                    res.add(new EfficiencyEquationSegment(segment.base, segment.coefficient, segment.start, factor).setOffset(segment.offset));
                    break;
                }
                else {
                    res.add(segment.clone());
                }
            }
            return new EfficiencyEquation(res).adjustSegmentOffsets().relinkSegments();
        }
        public EfficiencyEquation mult(double factor) {
            return new EfficiencyEquation(segments.stream().map(component -> component.mult(factor)).collect(Collectors.toList())).adjustSegmentOffsets().relinkSegments();
        }
        public EfficiencyEquation concat(EfficiencyEquation other) {
            List<EfficiencyEquationSegment> concatenated = segments.stream().map(EfficiencyEquationSegment::clone).collect(Collectors.toList());
            concatenated.addAll(other.segments.stream().map(EfficiencyEquationSegment::clone).collect(Collectors.toList()));
            return new EfficiencyEquation(concatenated).adjustSegmentOffsets().relinkSegments();
        }
        public EfficiencyEquation add(EfficiencyEquation other) {
            double epsilon = 1E-6;
            if (other.segments.size() == 0)
                return clone();
            if (segments.size() == 0)
                return other.clone();
            List<EfficiencyEquationSegment> res = new ArrayList<>();
            int i1 = 0;
            int i2 = 0;
            int i1prev = -1;
            int i2prev = -1;
            double position = 0;
            EfficiencyEquation e1 = shallowClone();
            EfficiencyEquation e2 = other.shallowClone();
            if (e1.segments.get(e1.segments.size() - 1).end != Double.POSITIVE_INFINITY)
                e1.segments.add(new EfficiencyEquationSegment(0, 0, e1.segments.get(e1.segments.size() - 1).end, Double.POSITIVE_INFINITY));
            if (e2.segments.get(e2.segments.size() - 1).end != Double.POSITIVE_INFINITY)
                e2.segments.add(new EfficiencyEquationSegment(0, 0, e2.segments.get(e2.segments.size() - 1).end, Double.POSITIVE_INFINITY));
            EfficiencyEquationSegment s1 = e1.segments.get(i1);
            EfficiencyEquationSegment s2 = e2.segments.get(i2);
            while (i1prev != i1 || i2prev != i2) {
                double nextPosition = Math.min(s1.end, s2.end);
                EfficiencyEquationSegment newSegment = new EfficiencyEquationSegment(s1.evaluateRelative(position) + s2.evaluateRelative(position), s1.coefficient + s2.coefficient, position, nextPosition);
                if (res.size() > 0 && Math.abs(res.get(res.size() - 1).evaluateRelative(position) - newSegment.evaluateRelative(position)) <= epsilon && Math.abs(res.get(res.size() - 1).coefficient - newSegment.coefficient) <= epsilon) {
                    res.get(res.size() - 1).end = newSegment.end;
                }
                else {
                    res.add(newSegment);
                }
                position = nextPosition;
                i1prev = i1;
                i2prev = i2;
                if (s1.end == s2.end) {
                    i1 = Math.min(e1.segments.size() - 1, i1 + 1);
                    i2 = Math.min(e2.segments.size() - 1, i2 + 1);
                } else if (s1.end < s2.end) {
                    i1 = Math.min(e1.segments.size() - 1, i1 + 1);
                } else {
                    i2 = Math.min(e2.segments.size() - 1, i2 + 1);
                }
                s1 = e1.segments.get(i1);
                s2 = e2.segments.get(i2);
            }
            if (!res.isEmpty() && res.get(res.size() - 1).base == 0 && res.get(res.size() - 1).end == Double.POSITIVE_INFINITY)
                res.remove(res.size() - 1);
            return new EfficiencyEquation(res).adjustSegmentOffsets().relinkSegments();
        }
        private double findIntersectionPoint(EfficiencyEquationSegment s1, EfficiencyEquationSegment s2) {
            double epsilon = 1E-6;
            double m = s1.coefficient;
            double n = s2.coefficient;
            double b = s1.offset + s1.base - m * s1.start;
            double d = s2.offset + s2.base - n * s2.start;
            if (Math.abs(m - n) <= epsilon)
                return 0;
            return (d - b) / (m - n);
        }
        public EfficiencyEquation min(EfficiencyEquation other) {
            double epsilon = 1E-6;
            if (other.segments.size() == 0)
                return clone();
            if (segments.size() == 0)
                return other.clone();
            List<EfficiencyEquationSegment> res = new ArrayList<>();
            int i1 = 0;
            int i2 = 0;
            int i1prev = -1;
            int i2prev = -1;
            double position = 0;
            EfficiencyEquation e1 = shallowClone();
            EfficiencyEquation e2 = other.shallowClone();
            if (e1.segments.get(e1.segments.size() - 1).end != Double.POSITIVE_INFINITY)
                e1.segments.add(new EfficiencyEquationSegment(Double.POSITIVE_INFINITY, 0, e1.segments.get(e1.segments.size() - 1).end, Double.POSITIVE_INFINITY));
            if (e2.segments.get(e2.segments.size() - 1).end != Double.POSITIVE_INFINITY)
                e2.segments.add(new EfficiencyEquationSegment(Double.POSITIVE_INFINITY, 0, e2.segments.get(e2.segments.size() - 1).end, Double.POSITIVE_INFINITY));
            EfficiencyEquationSegment s1 = e1.segments.get(i1);
            EfficiencyEquationSegment s2 = e2.segments.get(i2);
            while (i1prev != i1 || i2prev != i2) {
                double nextPosition = Math.min(s1.end, s2.end);
                double intersectionPoint = findIntersectionPoint(s1, s2);
                EfficiencyEquationSegment smallest = s1.evaluate(position) < s2.evaluate(position) ? s1 : s2;
                EfficiencyEquationSegment largest = smallest == s1 ? s2 : s1;
                EfficiencyEquationSegment priorIntersection;
                EfficiencyEquationSegment postIntersection = null;
                if (Math.abs(s1.coefficient - s2.coefficient) > epsilon && intersectionPoint >= position && intersectionPoint < nextPosition) {
                    priorIntersection = new EfficiencyEquationSegment(smallest.evaluateRelative(position), smallest.coefficient, position, intersectionPoint);
                    postIntersection = new EfficiencyEquationSegment(0, largest.coefficient, intersectionPoint, nextPosition);
                }
                else {
                    priorIntersection = new EfficiencyEquationSegment(smallest.evaluateRelative(position), smallest.coefficient, position, nextPosition);
                }
                if (res.size() > 0 && Math.abs(res.get(res.size() - 1).evaluateRelative(position) - priorIntersection.evaluateRelative(position)) <= epsilon && Math.abs(res.get(res.size() - 1).coefficient - priorIntersection.coefficient) <= epsilon) {
                    res.get(res.size() - 1).end = priorIntersection.end;
                }
                else {
                    res.add(priorIntersection);
                    if (postIntersection != null)
                        res.add(postIntersection);
                }
                position = nextPosition;
                i1prev = i1;
                i2prev = i2;
                if (s1.end == s2.end) {
                    i1 = Math.min(e1.segments.size() - 1, i1 + 1);
                    i2 = Math.min(e2.segments.size() - 1, i2 + 1);
                } else if (s1.end < s2.end) {
                    i1 = Math.min(e1.segments.size() - 1, i1 + 1);
                } else {
                    i2 = Math.min(e2.segments.size() - 1, i2 + 1);
                }
                s1 = e1.segments.get(i1);
                s2 = e2.segments.get(i2);
            }
            if (!res.isEmpty() && res.get(res.size() - 1).base == Double.POSITIVE_INFINITY && res.get(res.size() - 1).end == Double.POSITIVE_INFINITY)
                res.remove(res.size() - 1);
            return new EfficiencyEquation(res).adjustSegmentOffsets().relinkSegments();
        }
        private EfficiencyEquation adjustSegmentOffsets() {
            if (segments.size() == 0)
                return this;
            segments.get(0).setOffset(0);
            for (int i = 1; i < segments.size(); i++) {
                EfficiencyEquationSegment prev = segments.get(i - 1);
                EfficiencyEquationSegment cur = segments.get(i);
                cur.offset = prev.evaluate(prev.end);
            }
            return this;
        }
        private EfficiencyEquation relinkSegments() {
            for (int i = 1; i < segments.size(); i++) {
                EfficiencyEquationSegment prev = segments.get(i - 1);
                EfficiencyEquationSegment cur = segments.get(i);
                double diff = cur.start - prev.end;
                cur.start -= diff;
                if (cur.end != Double.POSITIVE_INFINITY)
                    cur.end -= diff;
            }
            return this;
        }
        public EfficiencyEquation clone() {
            return new EfficiencyEquation(segments.stream().map(EfficiencyEquationSegment::clone).collect(Collectors.toList()));
        }
        public EfficiencyEquation shallowClone() {
            return new EfficiencyEquation(new ArrayList<>(segments));
        }
    }

    public static abstract class Octree {
        protected int startX, startY, startZ;
        protected int size;
        protected Set<Block> contents;
        protected List<Octree> children;
        public static Set<Block> unusedBlocks = Set.of(Blocks.AIR, Blocks.VOID_AIR, Blocks.CAVE_AIR, Blocks.WATER, Blocks.LAVA);
        public Octree(int startX, int startY, int startZ, int size) {
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.size = size;
            contents = new LinkedHashSet<>();
            children = new ArrayList<>();
        }
        public static Octree construct(int startX, int startY, int startZ, int size) {
            if (size > 1)
                return new OctreeDivisible(startX, startY, startZ, size);
            return new OctreeLeaf(startX, startY, startZ);
        }
        public abstract boolean create();
        public boolean containsPointXZ(BlockPos pos) {
            int x = pos.getX(), z = pos.getZ();
            return x >= startX && x < startX + size && z >= startZ && z < startZ + size;
        }
        public boolean containsPoint(BlockPos pos) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            return x >= startX && x < startX + size && y >= startY && y < startY + size && z >= startZ && z < startZ + size;
        }
        public BlockPos getClosestPoint(BlockPos pos) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            if (x < startX)
                x = startX;
            if (x >= startX + size)
                x = startX + size - 1;
            if (y < startY)
                y = startY;
            if (y >= startY + size)
                y = startY + size - 1;
            if (z < startZ)
                z = startZ;
            if (z >= startZ + size)
                z = startZ + size - 1;
            return new BlockPos(x, y, z);
        }
        public BlockPos getQuadrantCoordinates(BlockPos pos) {
            int halfSize = size / 2;
            int x = startX + halfSize;
            int y = startY + halfSize;
            int z = startZ + halfSize;
            if (x > pos.getX())
                x -= halfSize;
            if (y > pos.getY())
                y -= halfSize;
            if (z > pos.getZ())
                z -= halfSize;
            return new BlockPos(x, y, z);
        }
        public BlockPos getFarthestPoint(BlockPos pos) {
            int i = 0, j = 0, k = 0;
            i = Math.abs(pos.getX() - startX) < Math.abs(pos.getX() - startX - size) ? 1 : 0;
            j = Math.abs(pos.getY() - startY) < Math.abs(pos.getY() - startY - size) ? 1 : 0;
            k = Math.abs(pos.getZ() - startZ) < Math.abs(pos.getZ() - startZ - size) ? 1 : 0;
            return new BlockPos(startX + size * i, startY + size * j, startZ + size * k);
        }
        public void calculateBlockDistances(LinkedHashMap<Octree, Double> closest, LinkedHashMap<Octree, Double> farthest, BlockPos pos) {
            BlockPos closestPos = getClosestPoint(pos);
            BlockPos farthestPos = getFarthestPoint(pos);
            double closestDist = new Vec3d(closestPos.getX(), closestPos.getY(), closestPos.getZ()).subtract(new Vec3d(pos.getX(), pos.getY(), pos.getZ())).length();
            double farthestDist = new Vec3d(farthestPos.getX(), farthestPos.getY(), farthestPos.getZ()).subtract(new Vec3d(pos.getX(), pos.getY(), pos.getZ())).length();
            closest.put(this, closestDist);
            farthest.put(this, farthestDist);
        }
        public void removeTree(LinkedHashMap<Octree, Double> closest, LinkedHashMap<Octree, Double> farthest) {
            closest.remove(this);
            farthest.remove(this);
        }
        public int treeBinarySearch(List<Octree> nodes, LinkedHashMap<Octree, Double> closestDistance, int l, int r) {
            double x = closestDistance.get(this);
            if (r >= l) {
                int mid = l + (r - l) / 2;
                if (closestDistance.get(nodes.get(mid)) == x)
                    return mid;
                if (closestDistance.get(nodes.get(mid)) > x)
                    return treeBinarySearch(nodes, closestDistance, l, mid - 1);
                return treeBinarySearch(nodes, closestDistance, mid + 1, r);
            }
            return l;
        }
        public BlockPos findClosest(Block block, BlockPos pos) {
            double epsilon = 1E-6;
            LinkedHashMap<Octree, Double> closestDistance = new LinkedHashMap<>();
            LinkedHashMap<Octree, Double> farthestDistance = new LinkedHashMap<>();
            List<Octree> activeNodes = new ArrayList<>();
            activeNodes.add(this);
            calculateBlockDistances(closestDistance, farthestDistance, pos);
            double minFarthest = farthestDistance.get(this);
            while (activeNodes.size() > 0) {
                Octree selection = null;
                for (int i = 0; i < activeNodes.size(); i++) {
                    if (activeNodes.get(i).children.size() > 0) {
                        selection = activeNodes.get(i);
                        activeNodes.remove(i);
                        selection.removeTree(closestDistance, farthestDistance);
                        break;
                    }
                }
                if (selection == null)
                    break;
                for (Octree child : selection.children) {
                    if (child.contents.contains(block)) {
                        child.calculateBlockDistances(closestDistance, farthestDistance, pos);
                        if (closestDistance.get(child) - epsilon > minFarthest) {
                            child.removeTree(closestDistance, farthestDistance);
                            continue;
                        }
                        minFarthest = Math.min(minFarthest, farthestDistance.get(child));
                        double furthest = farthestDistance.get(child);
                        for (int i = activeNodes.size() - 1; i >= 0; i--) {
                            if (closestDistance.get(activeNodes.get(i)) - epsilon > furthest) {
                                activeNodes.get(i).removeTree(closestDistance, farthestDistance);
                                activeNodes.remove(i);
                            }
                            else {
                                break;
                            }
                        }
                        int index = child.treeBinarySearch(activeNodes, closestDistance, 0, activeNodes.size() - 1);
                        activeNodes.add(index, child);
                    }
                }
            }
            if (activeNodes.size() == 0)
                return null;
            Octree leaf = activeNodes.get(0);
            return new BlockPos(leaf.startX, leaf.startY, leaf.startZ);
        }
        public void addBlock(Block block, BlockPos pos) {
            if (size == 1 || !containsPoint(pos))
                return;
            contents.add(block);
            for (Octree child : children) {
                if (child.containsPoint(pos)) {
                    child.addBlock(block, pos);
                    return;
                }
            }
            if (size == 2) {
                children.add(new OctreeLeaf(pos.getX(), pos.getY(), pos.getZ(), block));
            }
            else {
                int halfSize = size / 2;
                BlockPos quadrant = getQuadrantCoordinates(pos);
                Octree child = new OctreeDivisible(quadrant.getX(), quadrant.getY(), quadrant.getZ(), halfSize);
                children.add(child);
                child.addBlock(block, pos);
            }
        }
    }

    public static class OctreeDivisible extends Octree {
        public OctreeDivisible(int startX, int startY, int startZ, int size) {
            super(startX, startY, startZ, size);
        }
        public List<Octree> divide() {
            List<Octree> res = new ArrayList<>();
            int halfSize = size / 2;
            for (int i = 0; i <= 1; i++) {
                for (int j = 0; j <= 1; j++) {
                    for (int k = 0; k <= 1; k++) {
                        res.add(Octree.construct(startX + i * halfSize, startY + j * halfSize, startZ + k * halfSize, halfSize));
                    }
                }
            }
            return res;
        }
        @Override
        public boolean create() {
            children = divide();
            for (int i = children.size() - 1; i >= 0; i--) {
                Octree child = children.get(i);
                if (child.create()) {
                    contents.addAll(child.contents);
                }
                else {
                    children.remove(i);
                }
            }
            return !children.isEmpty();
        }
    }

    public static class OctreeLeaf extends Octree {
        public OctreeLeaf(int startX, int startY, int startZ) {
            super(startX, startY, startZ, 1);
        }
        public OctreeLeaf(int startX, int startY, int startZ, Block block) {
            this(startX, startY, startZ);
            contents = Set.of(block);
        }
        @Override
        public boolean create() {
            Block block = Blocks.VOID_AIR;
            try {
                BlockPos pos = new BlockPos(startX, startY, startZ);
                if (MC.world != null) {
                    BlockState state = MC.world.getBlockState(pos);
                    if (state != null)
                        block = state.getBlock();
                }
            }
            catch (NullPointerException ignored) { }
            contents = Set.of(block);
            return !Octree.unusedBlocks.contains(block);
        }
    }

    public Octree createBlockTree() {
        List<Chunk> chunks = worldQuery.getChunks(5);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Chunk chunk : chunks) {
            int bottomY = chunk.getBottomY();
            int topY = chunk.getTopY();
            int left = chunk.getPos().x * 16;
            int back = chunk.getPos().z * 16;
            int right = left + 16;
            int front = back + 16;
            if (bottomY < minY)
                minY = bottomY;
            if (topY > maxY)
                maxY = topY;
            if (left < minX)
                minX = left;
            if (back < minZ)
                minZ = back;
            if (right > maxX)
                maxX = right;
            if (front > maxZ)
                maxZ = front;
        }
        int sizeFactor = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        int size = (int)Math.pow(2, (int)Math.ceil(Math.log(sizeFactor) / Math.log(2)));
        Octree res = new OctreeDivisible(minX, minY, minZ, size);
        res.create();
        return res;
    }

    public abstract class Node {
        protected Item target;
        //protected int needed;
        protected List<CraftingProcess> processes;
        protected int stackShift;
        protected List<Node> children;
        //protected int naiveMaxCraftable;
        //protected boolean generatedNaiveMaxCraftable = false;
        //protected long nodeId;
        //protected int timeTaken;
        //protected int callCounter;
        private LinkedHashSet<Item> overlap = new LinkedHashSet<>();
        private LinkedHashSet<Item> resetOverlap = new LinkedHashSet<>();
        protected LinkedHashSet<Item> resetVisited = new LinkedHashSet<>();
        protected boolean shouldPruneTarget = true;
        protected boolean shouldOnlyCraftOne = false;
        protected boolean shouldResetVisited = false;
        public Class distinction = Node.class;
        private static int totalNumCalls = 0;
        private static int maxNodeId = 0;
        public Node(Item target, List<CraftingProcess> processes) {
            this.processes = processes;
            this.target = target;
            stackShift = 0;
        }
        public Node setDistinction(Class distinction) {
            this.distinction = distinction;
            return this;
        }
        public Node setShouldPruneTarget(boolean shouldPruneTarget) {
            this.shouldPruneTarget = shouldPruneTarget;
            return this;
        }
        public Node setShouldOnlyCraftOne(boolean shouldOnlyCraftOne) {
            this.shouldOnlyCraftOne = shouldOnlyCraftOne;
            return this;
        }
        public Node setShouldResetVisited(boolean shouldResetVisited) {
            this.shouldResetVisited = shouldResetVisited;
            return this;
        }
        protected boolean requiresAllChildren() {
            return true;
        }
        protected boolean requiresAnyChildren() {
            return true;
        }
        protected boolean shouldPruneNow(LinkedHashSet<Item> visited, LinkedHashSet<Item> resetVisited) {
            return false;
        }
        protected void mergeResources(Resources<OperableInteger> base, Resources<OperableInteger> res) {
            for (Long item : res.keySet()) {
                if (base.containsKey(item)) {
                    Pair<OperableInteger, ResourceDomain> baseItem = base.get(item);
                    baseItem.setLeft(baseItem.getLeft().add(res.get(item).getLeft()));
                }
                else {
                    base.put(item, res.get(item));
                }
            }
        }
        public Node setStackShift(int stackShift) {
            this.stackShift = stackShift;
            return this;
        }
        protected void clearDeadNodes(long id, CraftingState state) {
            if (state.neededMap.getOrDefault(id, 0) == 0)
                return;
            state.deadNodes.remove(id);
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = id + childIds.get(i);
                Node child = children.get(i);
                child.clearDeadNodes(childId, state);
            }
        }
        protected void clearEfficiencyEquations(CraftingState state) {
            if (!state.efficiencyEquations.containsKey(this))
                return;
            state.efficiencyEquations.remove(this);
            state.efficiencyEquationsGenerated.put(this, false);
            for (Node child : children) {
                child.clearEfficiencyEquations(state);
            }
        }
        protected void clearNaiveMaxCraftable(CraftingState state) {
            if (!state.naiveMaxCraftable.containsKey(this))
                return;
            state.naiveMaxCraftable.remove(this);
            state.naiveMaxCraftableGenerated.put(this, false);
            for (Node child : children) {
                child.clearNaiveMaxCraftable(state);
            }
        }
        private int getTotalExcessConsumed(long nodeId, CraftingState state) {
            int res = 0;
            LinkedHashMap<Long, Pair<Node, Integer>> entry = state.excessConsumed.getOrDefault(nodeId, new LinkedHashMap<>());
            for (Pair<Node, Integer> value : entry.values()) {
                res += value.getRight();
            }
            return res;
        }
        public void reformulateCraftingTree(long nodeId, CraftingState state) {
            clearNearestBlockMetrics();
            generateBlockMetrics();
            int originalNumNeeded = state.neededMap.getOrDefault(nodeId, 0) + getTotalExcessConsumed(nodeId, state);
            clearDeadNodes(nodeId, state);
            clearEfficiencyEquations(state);
            genEfficiencyEquations(state, new LinkedHashSet<>(), originalNumNeeded);
            clearNaiveMaxCraftable(state);
            genNaiveMaxCraftable(state);
            setExcessBlacklist(nodeId, state, new LinkedHashSet<>(), true);
            unconsumeResources(nodeId, nodeId + state.childScope.get(this), nodeId, state, new LinkedHashSet<>());
            setExcessBlacklist(nodeId, state, new LinkedHashSet<>(), false);
            clearNaiveMaxCraftable(state);
            genNaiveMaxCraftable(state);
            clearEfficiencyEquations(state);
            genEfficiencyEquations(state, new LinkedHashSet<>(), originalNumNeeded);
            Pair<Boolean, Resources<OperableInteger>> res = getBaseResources(nodeId, originalNumNeeded, originalNumNeeded, state.clone(), true, new LinkedHashSet<>());
            consumeResources(nodeId, res.getRight(), state, 0, new LinkedHashSet<>());
        }
        public boolean doCraft(long id, CraftingState state) {
            if (state.neededMap.getOrDefault(id, 0) == 0)
                return true;
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = id + childIds.get(i);
                Node child = children.get(i);
                if (state.neededMap.getOrDefault(childId, 0) > 0) {
                    child.reformulateCraftingTree(childId, state);
                }
                if (!child.doCraft(childId, state)) {
                    return false;
                }
            }
            return execute(id, state);
        }
        public void craft(int amount) {
            long startTime = System.currentTimeMillis();
            CraftingState state = createFreshState();
            calculateMaxCraftable(amount, state);
            long endTime = System.currentTimeMillis();
            System.out.println("TOTAL CALCULATION TIME: " + (int)(endTime - startTime));
            if (baritoneChatInterfaceEnabled.isChecked())
                baritoneChatInterface.setAcceptableThrowawayItems(state.throwawayItems.stream().toList());
            doCraft(state.rootNodeId, state);
            /*while (!doCraft())
                calculateMaxCraftable(amount);*/
        }
        protected abstract List<Node> getChildrenInternal(LinkedHashSet<Item> nodes);
        private List<Node> getChildren(CraftingState state) {
            List<Node> res = new ArrayList<>();
            if (processes == null)
                return res;
            for (Node child : getChildrenInternal(state.visited)) {
                if (!state.addVisited(state.visited, state.resetVisited, child))
                    continue;
                state.removeVisited(state.visited, state.resetVisited, child);
                res.add(child);
            }
            return res;
        }
        protected boolean shouldAlwaysConsiderPossible() {
            return false;
        }
        protected boolean canPossiblyCraft(CraftingState state) {
            return true;
        }
        private CraftingState createFreshState() {
            LinkedHashMap<Item, Integer> inventoryAvailability = (LinkedHashMap<Item, Integer>) inventoryAvailabilityMap.clone();
            LinkedHashMap<Item, Integer> storageAvailability = (LinkedHashMap<Item, Integer>) storageAvailabilityMap.clone();
            LinkedHashMap<Block, Integer> worldAvailability = (LinkedHashMap<Block, Integer>) worldAvailabilityMap.clone();
            CraftingState state = new CraftingState(inventoryAvailability, storageAvailability, worldAvailability, new LinkedHashSet<>());
            return state;
        }
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
                res = res.add(state.efficiencyEquations.get(child));
            }
            return res;
        }
        protected void genEfficiencyEquations(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            if (state.efficiencyEquationsGenerated.getOrDefault(this, false))
                return;
            /*if (shouldPruneTarget && visited.contains(target))
                return;
            if (shouldRememberVisit())
                visited.add(target);*/
            EfficiencyEquation equation = genEfficiencyEquationsInternal(state, visited, numNeeded);
            /*if (shouldRememberVisit())
                visited.remove(target);*/
            state.efficiencyEquations.put(this, equation);
            state.efficiencyEquationsGenerated.put(this, true);
        }
        private int calculateMaxCraftableInternal(int upperBound, boolean useHeuristic, boolean populateNaiveMetrics, CraftingState state) {
            CraftingState originalState = state;
            long treeStartTime = System.currentTimeMillis();
            if (!state.generateTree(0, this))
                return 0;
            long treeEndTime = System.currentTimeMillis();
            System.out.println("Tree generation time: " + (int)(treeEndTime - treeStartTime));
            if (populateNaiveMetrics) {
                containerQuery.calculateNearestContainer(MC.player.getPos());
                long equationStartTime = System.currentTimeMillis();
                genEfficiencyEquations(state, new LinkedHashSet<>(), 1);
                long equationEndTime = System.currentTimeMillis();
                System.out.println("Efficiency equation generation time: " + (int)(equationEndTime - equationStartTime));
            }
            System.out.println("Number of segments: " + state.efficiencyEquations.get(this).segments.size());
            CraftingState newState = state.clone();
            CraftingState clonedState = newState.clone();
            Pair<Boolean, Resources<OperableInteger>> resources = getBaseResources(state.rootNodeId, 1, 1, clonedState, useHeuristic, new LinkedHashSet<>());
            int amount = 0;
            while (amount < upperBound && resources.getLeft()) {
                state = newState;
                newState = state.clone();
                if (!consumeResources(state.rootNodeId, resources.getRight(), newState, 0, new LinkedHashSet<>())) {
                    newState = state.clone();
                    clonedState = newState.clone();
                    resources = getBaseResources(state.rootNodeId, 1, 1, clonedState, useHeuristic, new LinkedHashSet<>());
                }
                else {
                    amount++;
                }
            }
            reformulateCraftingTree(newState.rootNodeId, newState);
            originalState.set(newState);
            return amount;
        }
        private int getNumNodes(List<Node> ancestors) {
            int res = 1;
            if (ancestors.contains(this)) {
                int index = ancestors.indexOf(this);
                for (int i = index; i < ancestors.size(); i++) {
                    System.out.println(ancestors.get(i) + ", " + ancestors.get(i).target);
                }
                System.out.println("-----------------------------------------");
                ancestors.clear();
            }
            ancestors.add(this);
            for (Node child : children) {
                res += child.getNumNodes(ancestors);
            }
            ancestors.remove(ancestors.size() - 1);
            return res;
        }
        private int print(int indent, boolean ignoreNeeded, PrintStream stream, long id, CraftingState state) {
            int needed = state.neededMap.getOrDefault(id, 0);
            int timeTaken = state.timeTaken.getOrDefault(id, 0);
            double efficiencyEquationMetric = state.efficiencyEquations.containsKey(this) ? state.efficiencyEquations.get(this).evaluate(needed) : 0;
            double regularEfficiencyMetric = state.naiveEfficiencyMap.getOrDefault(this, 0.0);
            if (ignoreNeeded || state.neededMap.getOrDefault(id, 0) > 0) {
                String indentation = "";
                for (int i = 0; i < indent; i++) {
                    indentation += " ";
                }
                stream.println(indentation + target + ": " + needed + ", time " + timeTaken + ", " + this.getClass() + ", equation " + efficiencyEquationMetric + ", regular " + regularEfficiencyMetric);
                int calls = 1;
                List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
                for (int i = 0; i < children.size(); i++) {
                    long childId = id + childIds.get(i);
                    Node child = children.get(i);
                    calls += child.print(indent + 1, ignoreNeeded, stream, childId, state);
                }
                return calls;
            }
            return 0;
        }
        private int calculateMaxCraftable(int upperBound, CraftingState state) {
            globalTimeTaken = BigInteger.ZERO;
            int amount = calculateMaxCraftableInternal(upperBound, true, true, state);
            int numPrinted = print(0, false, System.out, state.rootNodeId, state);
            System.out.println("Total number of nodes: " + maxNodeId);
            System.out.println("Number of nodes printed: " + numPrinted);
            System.out.println("Craftable: " + amount);
            System.out.println("Nodes in tree: " + state.childScope.get(this));
            System.out.println("Total number of calls: " + totalNumCalls);
            BigInteger[] arr = globalTimeTaken.divideAndRemainder(new BigInteger("1000000000"));
            System.out.println("Time taken: " + arr[0] + ", " + arr[1]);
            return amount;
        }
        private int excessGetOrDefault(long nodeId, int defaultValue, List<Pair<Long, Pair<Node, Integer>>> excessItem) {
            for (int i = 0; i < excessItem.size(); i++) {
                Pair<Long, Pair<Node, Integer>> pair = excessItem.get(i);
                if (pair.getLeft() == nodeId)
                    return pair.getRight().getRight();
            }
            return defaultValue;
        }
        private void excessPut(long nodeId, Node node, int value, List<Pair<Long, Pair<Node, Integer>>> excessItem) {
            int index = 0;
            for (int i = 0; i < excessItem.size(); i++) {
                Pair<Long, Pair<Node, Integer>> pair = excessItem.get(i);
                if (pair.getLeft() == nodeId) {
                    pair.getRight().setRight(value);
                    if (value == 0)
                        excessItem.remove(i);
                    return;
                }
                else if (pair.getLeft() < nodeId) {
                    index = i;
                    break;
                }
            }
            if (value > 0)
                excessItem.add(index, new Pair<>(nodeId, new Pair<>(node, value)));
        }
        protected void setExcessBlacklist(long nodeId, CraftingState state, LinkedHashSet<Item> visited, boolean blacklist) {
            if (blacklist) {
                if (state.neededMap.getOrDefault(nodeId, 0) == 0)
                    return;
            }
            else {
                if (!state.excessBlacklist.contains(nodeId))
                    return;
            }
            if (blacklist) {
                state.excessBlacklist.add(nodeId);
            }
            else {
                state.excessBlacklist.remove(nodeId);
            }
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                child.setExcessBlacklist(childId, state, visited, blacklist);
            }
        }
        protected boolean unconsumeResources(long nodeId, long lowerBoundId, long upperBoundId, CraftingState state, LinkedHashSet<Item> visited) {
            boolean modified = state.excessProduced.containsKey(nodeId) ||
                    state.excessConsumed.containsKey(nodeId) ||
                    state.resourceConsumed.containsKey(nodeId) ||
                    state.inventoryConsumed.containsKey(nodeId) ||
                    state.storageConsumed.containsKey(nodeId) ||
                    state.worldConsumed.containsKey(nodeId);
            if (!modified && state.neededMap.getOrDefault(nodeId, 0) == 0)
                return true;
            if (excessGetOrDefault(nodeId, 0, state.excess.getOrDefault(target, new ArrayList<>())) > 0)
                excessPut(nodeId, this, 0, state.excess.get(target));
            if (state.excessProduced.containsKey(nodeId)) {
                LinkedHashMap<Long, Pair<Node, Integer>> value = state.excessProduced.get(nodeId);
                for (Long key : value.keySet()) {
                    state.excessConsumed.getOrDefault(key, new LinkedHashMap<>()).remove(nodeId);
                    if (key >= upperBoundId)
                        continue;
                    Pair<Node, Integer> pair = value.get(key);
                    Pair<Boolean, Resources<OperableInteger>> res = pair.getLeft().getBaseResources(key, pair.getRight(), pair.getRight(), state.clone(), true, new LinkedHashSet<>());
                    if (!res.getLeft()) {
                        if (shouldRememberVisit())
                            visited.remove(target);
                        return false;
                    }
                    if (!pair.getLeft().consumeResources(key, res.getRight(), state, 0, new LinkedHashSet<>())) {
                        if (shouldRememberVisit())
                            visited.remove(target);
                        return false;
                    }
                }
                state.excessProduced.remove(nodeId);
            }
            if (state.excessConsumed.containsKey(nodeId)) {
                LinkedHashMap<Long, Pair<Node, Integer>> value = state.excessConsumed.get(nodeId);
                for (Long key : value.keySet()) {
                    state.excessProduced.getOrDefault(key, new LinkedHashMap<>()).remove(nodeId);
                    if (key <= lowerBoundId)
                        continue;
                    Pair<Node, Integer> pair = value.get(key);
                    List<Pair<Long, Pair<Node, Integer>>> excessEntry = state.excess.getOrDefault(pair.getLeft().target, new ArrayList<>());
                    excessPut(key, pair.getLeft(), excessGetOrDefault(key, 0, excessEntry) + pair.getRight(), excessEntry);
                }
                state.excessConsumed.remove(nodeId);
            }
            if (state.resourceConsumed.containsKey(nodeId)) {
                state.neededMap.put(nodeId, state.neededMap.getOrDefault(nodeId, 0) - state.resourceConsumed.get(nodeId));
                if (state.neededMap.get(nodeId) == 0)
                    state.neededMap.remove(nodeId);
                state.resourceConsumed.remove(nodeId);
            }
            if (state.inventoryConsumed.containsKey(nodeId)) {
                state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) + state.inventoryConsumed.get(nodeId));
                state.inventoryConsumed.remove(nodeId);
            }
            if (state.storageConsumed.containsKey(nodeId)) {
                state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) + state.storageConsumed.get(nodeId));
                state.storageConsumed.remove(nodeId);
            }
            if (state.worldConsumed.containsKey(nodeId)) {
                state.worldAvailability.put(((WorldNode) this).block, state.worldAvailability.getOrDefault(((WorldNode) this).block, 0) + state.worldConsumed.get(nodeId));
                state.worldConsumed.remove(nodeId);
            }
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (!child.unconsumeResources(childId, lowerBoundId, upperBoundId, state, visited)) {
                    if (shouldRememberVisit())
                        visited.remove(target);
                    return false;
                }
            }
            return true;
        }
        protected boolean consumeResources(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
             if (shouldOnlyCraftOne)
                excessOverflow = 0;
            int originalExcessOverflow = excessOverflow;
            if (!state.deadNodes.contains(nodeId) && resources.containsKey(nodeId) && resources.get(nodeId).getLeft().getValue() > 0) {
                Pair<OperableInteger, ResourceDomain> resource = resources.get(nodeId);
                ResourceDomain domain = resource.getRight();
                int numNeeded = resource.getLeft().getValue();
                if (numNeeded - excessOverflow <= 0)
                    return true;
                if (state.excess.containsKey(target)) {
                    List<Pair<Long, Pair<Node, Integer>>> itemList = state.excess.get(target);
                    for (int i = itemList.size() - 1; i >= 0; i--) {
                        Pair<Long, Pair<Node, Integer>> pair = itemList.get(i);
                        if (state.excessBlacklist.contains(pair.getLeft()))
                            continue;
                        if (pair.getLeft() >= nodeId) {
                            int reductionFactor = Math.min(numNeeded, pair.getRight().getRight());
                            numNeeded -= reductionFactor;
                            excessOverflow += reductionFactor;
                            pair.getRight().setRight(pair.getRight().getRight() - reductionFactor);
                            if (!state.excessProduced.containsKey(pair.getLeft()))
                                state.excessProduced.put(pair.getLeft(), new LinkedHashMap<>());
                            if (!state.excessConsumed.containsKey(nodeId))
                                state.excessConsumed.put(nodeId, new LinkedHashMap<>());
                            if (!state.excessProduced.get(pair.getLeft()).containsKey(nodeId))
                                state.excessProduced.get(pair.getLeft()).put(nodeId, new Pair<>(this, 0));
                            if (!state.excessConsumed.get(nodeId).containsKey(pair.getLeft()))
                                state.excessConsumed.get(nodeId).put(pair.getLeft(), new Pair<>(this, 0));
                            Pair<Node, Integer> productionInfo = state.excessProduced.get(pair.getLeft()).get(nodeId);
                            Pair<Node, Integer> consumptionInfo = state.excessConsumed.get(nodeId).get(pair.getLeft());
                            productionInfo.setRight(productionInfo.getRight() + reductionFactor);
                            consumptionInfo.setRight(consumptionInfo.getRight() + reductionFactor);
                            if (pair.getRight().getRight() == 0)
                                itemList.remove(i);
                        }
                        if (numNeeded <= 0)
                            break;
                    }
                }
                int originalNeeded = state.neededMap.getOrDefault(nodeId, 0);
                int outputCount = getOutputCount();
                if (outputCount > 1) {
                    int neededToCraft = getNeededToCraft(originalNeeded + numNeeded);
                    int oldNeededToCraft = getNeededToCraft(originalNeeded);
                    int leftover = neededToCraft - (originalNeeded + numNeeded);
                    int oldLeftover = oldNeededToCraft - originalNeeded;
                    int leftoverDifference = leftover - oldLeftover;
                    state.excess.put(target, state.excess.getOrDefault(target, new ArrayList<>()));
                    int excessAmount = excessGetOrDefault(nodeId, 0, state.excess.get(target)) + leftoverDifference;
                    int shift = (int) Math.floor((double) excessAmount / outputCount) * outputCount;
                    excessPut(nodeId, this, excessAmount - shift, state.excess.get(target));
                    numNeeded -= shift;
                }
                state.resourceConsumed.put(nodeId, state.resourceConsumed.getOrDefault(nodeId, 0) + numNeeded - originalExcessOverflow);
                state.neededMap.put(nodeId, originalNeeded + numNeeded - originalExcessOverflow);
                int effectiveNeeded = numNeeded - excessOverflow;
                if (domain == ResourceDomain.INVENTORY) {
                    if (state.inventoryAvailability.getOrDefault(target, 0) < effectiveNeeded)
                        return false;
                    state.inventoryConsumed.put(nodeId, state.inventoryConsumed.getOrDefault(nodeId, 0) + effectiveNeeded);
                    state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) - effectiveNeeded);
                } else if (domain == ResourceDomain.STORAGE) {
                    if (state.storageAvailability.getOrDefault(target, 0) < effectiveNeeded)
                        return false;
                    state.storageConsumed.put(nodeId, state.storageConsumed.getOrDefault(nodeId, 0) + effectiveNeeded);
                    state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) - effectiveNeeded);
                } else if (domain == ResourceDomain.WORLD) {
                    if (state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) < effectiveNeeded)
                        return false;
                    state.worldConsumed.put(nodeId, state.worldConsumed.getOrDefault(nodeId, 0) + effectiveNeeded);
                    state.worldAvailability.put(((WorldCraftingProcess) processes.get(0)).block, state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) - effectiveNeeded);
                }
                if (shouldOnlyCraftOne)
                    state.deadNodes.add(nodeId);
                return consumeResourcesInternal(nodeId, resources, state, excessOverflow, visited);
            }
            return true;
        }
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (!child.consumeResources(childId, resources, state, excessOverflow, visited))
                    return false;
            }
            return true;
        }
        protected Resources<OperableInteger> stackResourcesBase(long nodeId, CraftingState state, int factor, Resources<OperableInteger> resources, LinkedHashSet<Item> visited) {
            Resources<OperableInteger> res = new Resources<>();
            int nodeValue = resources.getOrDefault(nodeId, new Pair<>(new OperableInteger(1), ResourceDomain.COMPOSITE)).getLeft().getValue();
            stackResources(nodeId, state, factor * nodeValue, res, resources, visited);
            return res;
        }
        protected void stackResources(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            if (!src.containsKey(nodeId))
                return;
            stackResourcesInternal(nodeId, state, num, dest, src, visited);
        }
        protected abstract void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited);
        private Pair<Boolean, Resources<OperableInteger>> getBaseResources(long nodeId, int numNeeded, int actualNeeded, CraftingState state, boolean useHeuristic, LinkedHashSet<Item> visited) {
            if (state.deadNodes.contains(nodeId))
                return new Pair<>(true, new Resources<>());
            if (!state.idInstanceMap.containsKey(nodeId))
                state.idInstanceMap.put(nodeId, this);
            if (shouldOnlyCraftOne) {
                numNeeded = Math.min(numNeeded, 1);
                actualNeeded = Math.min(actualNeeded, 1);
            }
            long startTime = System.currentTimeMillis();
            Pair<Boolean, Resources<OperableInteger>> result = new Pair<>(true, new Resources<>());
            /*if (shouldPruneTarget && visited.contains(target))
                return new Pair<>(false, new Resources<>());
            if (shouldRememberVisit())
                visited.add(target);*/
            if (state.excess.containsKey(target)) {
                List<Pair<Long, Pair<Node, Integer>>> targetExcess = state.excess.get(target);
                for (int i = targetExcess.size() - 1; i >= 0; i--) {
                    Pair<Long, Pair<Node, Integer>> pair = targetExcess.get(i);
                    if (actualNeeded == 0)
                        break;
                    if (pair.getLeft() >= nodeId) {
                        int nodeValue = pair.getRight().getRight();
                        int reductionFactor = Math.min(actualNeeded, nodeValue);
                        actualNeeded -= reductionFactor;
                        pair.getRight().setRight(nodeValue - reductionFactor);
                        if (pair.getRight().getRight() == 0)
                            targetExcess.remove(i);
                    }
                }
            }
            /*if (numNeeded == 1 && state.cachedResources.containsKey(this)) {
                Resources<OperableInteger> cachedRes = shiftResources(state.cachedResources.get(this), nodeId);
                if (actualNeeded == 1 && consumeResources(nodeId, cachedRes, state.clone(), 0, visited)) {
                    if (shouldRememberVisit())
                        visited.remove(target);
                    state.efficiencyMap.put(nodeId, state.cachedEfficiency.get(this));
                    return new Pair<>(true, cachedRes);
                }
            }*/
            if (numNeeded > 0)
                result = getBaseResourcesInternal(nodeId, numNeeded, actualNeeded, state, useHeuristic, visited);
            //calculateExecutionTime(nodeId, result.getRight(), state);
            /*if (numNeeded == 1 && result.getLeft()) {
                if (state.deadNodes.contains(nodeId)) {
                    state.cachedResources.put(this, new Resources<>());
                    state.cachedEfficiency.put(this, 0.0);
                }
                else {
                    state.cachedResources.put(this, shiftResources(result.getRight(), -nodeId));
                    state.cachedEfficiency.put(this, state.efficiencyMap.get(nodeId));
                }
            }*/
            long endTime = System.currentTimeMillis();
            state.timeTaken.put(nodeId, (int)(endTime - startTime));
            totalNumCalls++;
            /*if (shouldRememberVisit())
                visited.remove(target);*/
            return result;
        }
        protected boolean drawResourcesFromState(CraftingState state, int numNeeded, int actualNeeded) {
            return true;
        }
        protected int getChildNeededFactor(Node child, int numNeeded) {
            return numNeeded;
        }
        protected ResourceDomain getResourceDomain() {
            return ResourceDomain.COMPOSITE;
        }
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(long nodeId, int numNeeded, int actualNeeded, CraftingState state, boolean useHeuristic, LinkedHashSet<Item> visited) {
            Resources<OperableInteger> res = new Resources<>();
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(childId, getChildNeededFactor(child, numNeeded), getChildNeededFactor(child, actualNeeded), state, useHeuristic, visited);
                if (!childRes.getLeft())
                    return new Pair<>(false, res);
                mergeResources(res, childRes.getRight());
            }
            ResourceDomain domain = getResourceDomain();
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), domain));
            return new Pair<>(drawResourcesFromState(state, numNeeded, actualNeeded), res);
        }
        protected abstract void genNaiveMaxCraftableInternal(CraftingState state);
        protected void genNaiveMaxCraftable(CraftingState state) {
            if (state.naiveMaxCraftableGenerated.getOrDefault(this, false))
                return;
            if (!state.addVisited(state.visited, state.resetVisited, this))
                return;
            state.pushVisitedClone(this);
            for (Node child : children) {
                child.genNaiveMaxCraftable(state);
            }
            genNaiveMaxCraftableInternal(state);
            state.naiveMaxCraftableGenerated.put(this, true);
            state.popVisitedClone(this);
            state.removeVisited(state.visited, state.resetVisited, this);
        }
        protected abstract boolean shouldRememberVisit();
        public abstract LinkedHashMap<Item, ItemStack> collectIngredients();
        public abstract boolean execute(long nodeId, CraftingState state);
        public abstract int getOutputCount();
        public int getNeededToCraft(int amount) {
            return (int)Math.ceil((double)amount / getOutputCount()) * getOutputCount();
        }
        protected int hashCodeInternal(int hash) {
            return hash;
        }
        protected int equivalenceClassHashCodeInternal(int hash) {
            return hash;
        }
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + stackShift;
            hash = 31 * hash + target.hashCode();
            hash = 31 * hash + getClass().hashCode();
            hash = 31 * hash + distinction.hashCode();
            hash = 31 * hash + overlap.hashCode();
            hash = 31 * hash + resetVisited.hashCode();
            hash = hashCodeInternal(hash);
            return hash;
        }
        public int equivalenceClassHashCode() {
            int hash = 7;
            hash = 31 * hash + stackShift;
            hash = 31 * hash + target.hashCode();
            hash = 31 * hash + getClass().hashCode();
            hash = 31 * hash + distinction.hashCode();
            hash = equivalenceClassHashCodeInternal(hash);
            return hash;
        }
        protected boolean equalsInternal(Object other) {
            return true;
        }
        protected boolean equivalenceClassEqualsInternal(Object other) {
            return true;
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof Node o) {
                return overlap.size() == o.overlap.size() && resetVisited.size() == o.resetVisited.size() && target.equals(o.target) && stackShift == o.stackShift && getClass().equals(o.getClass()) && distinction.equals(o.distinction) && equalsInternal(other) && overlap.equals(o.overlap) && resetVisited.equals(o.resetVisited);
            }
            return false;
        }
        public boolean equivalenceClassEquals(Object other) {
            if (other instanceof Node o) {
                return target.equals(o.target) && stackShift == o.stackShift && getClass().equals(o.getClass()) && distinction.equals(o.distinction) && equivalenceClassEqualsInternal(other);
            }
            return false;
        }
        protected abstract Node cloneInternal();
        public Node clone() {
            Node cloned = cloneInternal();
            cloned.target = target;
            cloned.stackShift = stackShift;
            cloned.processes = new ArrayList<>(processes);
            cloned.children = new ArrayList<>(children);
            cloned.overlap = (LinkedHashSet<Item>) overlap.clone();
            cloned.resetOverlap = (LinkedHashSet<Item>) resetOverlap.clone();
            cloned.resetVisited = (LinkedHashSet<Item>) resetVisited.clone();
            cloned.shouldPruneTarget = shouldPruneTarget;
            cloned.shouldOnlyCraftOne = shouldOnlyCraftOne;
            cloned.shouldResetVisited = shouldResetVisited;
            cloned.distinction = distinction;
            return cloned;
        }
    }

    private boolean areNodesEquivalent(Node n1, Node n2) {
        return new NodeEquivalenceClass(n1).equals(new NodeEquivalenceClass(n2));
    }

    public class SmithingNode extends Node {
        private Node netherite;
        private Node smithingTable;
        public SmithingNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    if (areNodesEquivalent(child, smithingTable)) {
                        child.stackResources(childId, state, 1, dest, src, visited);
                    }
                    else {
                        child.stackResources(childId, state, num, dest, src, visited);
                    }
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            List<Node> res = new ArrayList<>();
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            for (ItemStack stack : ingredients.values()) {
                Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
                CraftingPlan plan = getCraftingPlan(itemIdentifier.getPath());
                Node child = plan.getNode();
                res.add(child);
            }
            Identifier netheriteIdentifier = new Identifier("minecraft", "netherite_ingot");
            netherite = getCraftingPlan(netheriteIdentifier.getPath()).getNode();
            res.add(netherite);
            Block smithingTableBlock = Registry.BLOCK.get(new Identifier("minecraft", "smithing_table"));
            smithingTable = new WorkbenchCraftingProcess(smithingTableBlock).getNode();
            res.add(smithingTable);
            return res;
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
                if (areNodesEquivalent(child, smithingTable)) {
                    res = res.add(new EfficiencyEquation(List.of(new EfficiencyEquationSegment(state.efficiencyEquations.get(child).evaluate(1), 0, 0, Integer.MAX_VALUE))));
                }
                else {
                    res = res.add(state.efficiencyEquations.get(child));
                }
            }
            return res;
        }
        @Override
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child =  children.get(i);
                if (!child.consumeResources(childId, resources, state, 0, visited))
                    return false;
            }
            return true;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            if (children.size() == 0) {
                state.naiveMaxCraftable.put(this, 0);
                return;
            }
            int outputFactor = Integer.MAX_VALUE;
            for (Node child : children) {
                outputFactor = Math.min(outputFactor, state.naiveMaxCraftable.get(child));
            }
            state.naiveMaxCraftable.put(this, outputFactor);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            LinkedHashMap<Item, ItemStack> stackTypes = new LinkedHashMap<>();
            Item diamondItem = Registry.ITEM.get(new Identifier("minecraft", Registry.ITEM.getId(target).getPath().replace("netherite", "diamond")));
            stackTypes.put(diamondItem, new ItemStack(diamondItem, 1));
            return stackTypes;
        }
        @Override
        public int getOutputCount() {
            return ((SmithingCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
        }
        private int calculateCraftingOutput() {
            return 1;
        }
        private void adjustTotalAvailability(Recipe<?> recipe, int craftingOutput) {
            Item outputItem = recipe.getOutput().getItem();
            synchronized (totalInventoryAvailabilityMap) {
                totalInventoryAvailabilityMap.put(outputItem, totalInventoryAvailabilityMap.getOrDefault(outputItem, 0) + craftingOutput);
            }
        }
        private void placeInsideSlot(Item item, int slot, int count) {
            List<Slot> slots = MC.player.currentScreenHandler.slots;
            for (int i = slots.size() - 37; i < slots.size(); i++) {
                if (count <= 0)
                    break;
                if (slots.get(i).getStack().getItem().equals(item)) {
                    int slotAmount = slots.get(i).getStack().getCount();
                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                    if (slotAmount <= count) {
                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, MC.player);
                        count -= slotAmount;
                    } else {
                        for (int j = 0; j < count; j++) {
                            MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, slot, 1, SlotActionType.PICKUP, MC.player);
                        }
                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, MC.player);
                        count = 0;
                    }
                }
            }
        }
        private void arrangeRecipe(int craftAmount) {
            int regularSlot = 0;
            for (ItemStack stack : collectIngredients().values()) {
                placeInsideSlot(stack.getItem(), regularSlot, craftAmount);
            }
            Item netheriteItem = Registry.ITEM.get(new Identifier("minecraft", "netherite_ingot"));
            int netheriteSlot = 1;
            placeInsideSlot(netheriteItem, netheriteSlot, craftAmount);
        }
        @Override
        protected int hashCodeInternal(int hash) {
            return 31 * hash + ((SmithingCraftingProcess) processes.get(0)).recipe.hashCode();
        }
        @Override
        protected Node cloneInternal() {
            SmithingNode res = new SmithingNode(target, processes);
            res.netherite = netherite;
            res.smithingTable = smithingTable;
            return res;
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            if (!usingSmithingTable()) {
                Block smithingTableBlock = Registry.BLOCK.get(new Identifier("minecraft", "smithing_table"));
                BlockPos nearestSmithingTable = containerManager.getClosestToPlayer(smithingTableBlock);
                if (nearestSmithingTable != null)
                    containerManager.navigateAndOpenContainer(nearestSmithingTable);
            }
            int neededToCraft = state.neededMap.getOrDefault(nodeId, 0);
            int craftingOutput;
            while (neededToCraft > 0 && (craftingOutput = calculateCraftingOutput()) > 0) {
                int amount = Math.min(craftingOutput, neededToCraft);
                adjustTotalAvailability(((SmithingCraftingProcess)processes.get(0)).recipe, amount);
                if (!usingSmithingTable()) return false;
                arrangeRecipe(amount / ((SmithingCraftingProcess) processes.get(0)).recipe.getOutput().getCount());
                if (!awaitSlotUpdate(((SmithingCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), amount, 2, true, false, true))
                    return false;
                if (!usingSmithingTable()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 2, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false, false);
                neededToCraft -= amount;
            }
            return true;
        }
    }

    public class SmeltingNode extends Node {
        private Node fuel;
        private Node furnace;
        public SmeltingNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    if (areNodesEquivalent(child, furnace)) {
                        child.stackResources(childId, state, 1, dest, src, visited);
                    }
                    else if (areNodesEquivalent(child, fuel)) {
                        child.stackResources(childId, state, (num / 8) + (num % 8 > 0 ? 1 : 0), dest, src, visited);
                    }
                    else {
                        child.stackResources(childId, state, num, dest, src, visited);
                    }
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            List<Node> res = new ArrayList<>();
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            for (ItemStack stack : ingredients.values()) {
                Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
                CraftingPlan plan = getCraftingPlan(itemIdentifier.getPath());
                Node child = plan.getNode();
                res.add(child);
            }
            Identifier coalIdentifier = new Identifier("minecraft", "coal");
            fuel = getCraftingPlan(coalIdentifier.getPath()).getNode();
            res.add(fuel);
            Block furnaceBlock = Registry.BLOCK.get(new Identifier("minecraft", "furnace"));
            furnace = new WorkbenchCraftingProcess(furnaceBlock).getNode();
            res.add(furnace);
            return res;
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, getChildNeededFactor(child, numNeeded));
                if (areNodesEquivalent(child, fuel)) {
                    res = res.add(state.efficiencyEquations.get(child).mult(1.0 / 8.0));
                }
                else if (areNodesEquivalent(child, furnace)) {
                    res = res.add(new EfficiencyEquation(List.of(new EfficiencyEquationSegment(state.efficiencyEquations.get(child).evaluate(1), 0, 0, Integer.MAX_VALUE))));
                }
                else {
                    res = res.add(state.efficiencyEquations.get(child));
                }
            }
            return res;
        }
        @Override
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            List<Long> childIds = state.children.get(this);
            int numNeeded = resources.get(nodeId).getLeft().getValue();
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child =  children.get(i);
                int childNeeded = 0;
                if (resources.containsKey(childId))
                    childNeeded = resources.get(childId).getLeft().getValue();
                if (!child.consumeResources(childId, resources, state, getChildNeededFactor(child, childNeeded) - getChildNeededFactor(child, numNeeded - excessOverflow), visited))
                    return false;
            }
            return true;
        }
        @Override
        protected int getChildNeededFactor(Node child, int num) {
            if (areNodesEquivalent(child, fuel))
                return (num / 8) + (num % 8 > 0 ? 1 : 0);
            return num;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            if (children.size() == 0) {
                state.naiveMaxCraftable.put(this, 0);
                return;
            }
            int outputFactor = Integer.MAX_VALUE;
            for (Node child : children) {
                outputFactor = Math.min(outputFactor, state.naiveMaxCraftable.get(child) * (areNodesEquivalent(child, fuel) ? 8 : 1));
            }
            state.naiveMaxCraftable.put(this, outputFactor);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            LinkedHashMap<Item, ItemStack> stackTypes = new LinkedHashMap<>();
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
            LinkedHashMap<Item, ItemStack> collected = collectIngredients();
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
            /*for (Ingredient ing : recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                synchronized (totalInventoryAvailabilityMap) {
                    totalInventoryAvailabilityMap.put(stack.getItem(), totalInventoryAvailabilityMap.getOrDefault(stack.getItem(), 0) - (stack.getCount() * craftingOutput) / recipe.getOutput().getCount());
                }
            }*/
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
                    int slot = slotY * craftingGridSize + slotX;
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
            int fuelSlot = 1;
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
        protected int hashCodeInternal(int hash) {
            return 31 * hash + ((SmeltingCraftingProcess) processes.get(0)).recipe.hashCode();
        }
        @Override
        protected Node cloneInternal() {
            SmeltingNode res = new SmeltingNode(target, processes);
            res.fuel = fuel;
            res.furnace = furnace;
            return res;
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            if (!usingFurnace()) {
                Block furnaceBlock = Registry.BLOCK.get(new Identifier("minecraft", "furnace"));
                BlockPos nearestFurnace = containerManager.getClosestToPlayer(furnaceBlock);
                if (nearestFurnace != null)
                    containerManager.navigateAndOpenContainer(nearestFurnace);
            }
            int neededToCraft = state.neededMap.getOrDefault(nodeId, 0);
            int craftingOutput;
            while (neededToCraft > 0 && (craftingOutput = calculateCraftingOutput()) > 0) {
                int amount = Math.min(craftingOutput, neededToCraft);
                adjustTotalAvailability(((SmeltingCraftingProcess)processes.get(0)).recipe, amount);
                if (!usingFurnace()) return false;
                arrangeRecipe(((SmeltingCraftingProcess) processes.get(0)).recipe, amount / ((SmeltingCraftingProcess) processes.get(0)).recipe.getOutput().getCount());
                if (!awaitSlotUpdate(((SmeltingCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), amount, 2, false, false, true))
                    return false;
                if (!usingFurnace()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 2, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false, false);
                neededToCraft -= amount;
            }
            return true;
        }
    }

    public class RecipeNode extends Node {
        public RecipeNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            int neededToCraft = getNeededToCraft(num);
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child =  children.get(i);
                if (src.containsKey(childId)) {
                    if (ingredients.containsKey(child.target)) {
                        child.stackResources(childId, state, (neededToCraft * ingredients.get(child.target).getCount()) / process.recipe.getOutput().getCount(), dest, src, visited);
                    }
                    else {
                        child.stackResources(childId, state, 1, dest, src, visited);
                    }
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            List<Node> res = new ArrayList<>();
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            for (ItemStack stack : ingredients.values()) {
                Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
                CraftingPlan plan = getCraftingPlan(itemIdentifier.getPath());
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
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            int naiveMaxCraftable = state.naiveMaxCraftable.get(this);
            EfficiencyEquation res = new EfficiencyEquation(List.of(new EfficiencyEquationSegment(0.05, 0, 0, naiveMaxCraftable)));
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, getChildNeededFactor(child, numNeeded));
                if (ingredients.containsKey(child.target)) {
                    res = res.add(state.efficiencyEquations.get(child).mult((double)ingredients.get(child.target).getCount() / process.recipe.getOutput().getCount()).clamp(naiveMaxCraftable));
                }
                else {
                    res = res.add(new EfficiencyEquation(List.of(new EfficiencyEquationSegment(state.efficiencyEquations.get(child).evaluate(1), 0, 0, naiveMaxCraftable))));
                }
            }
            return res;
        }
        @Override
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
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
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (!ingredients.containsKey(child.target)) {
                    if (!child.consumeResources(childId, resources, state, 0, visited))
                        return false;
                }
                else if (!child.consumeResources(childId, resources, state, difference * ingredients.get(child.target).getCount(), visited))
                    return false;
            }
            return true;
        }
        @Override
        protected int getChildNeededFactor(Node child, int numNeeded) {
            RecipeCraftingProcess process = (RecipeCraftingProcess) processes.get(0);
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            int neededToCraft = getNeededToCraft(numNeeded);
            if (child instanceof WorkbenchNode || child instanceof ToolNode)
                return Math.min(1, numNeeded);
            return ((neededToCraft * ingredients.get(child.target).getCount()) / process.recipe.getOutput().getCount());
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            if (children.size() == 0) {
                state.naiveMaxCraftable.put(this, 0);
                return;
            }
            int outputFactor = Integer.MAX_VALUE;
            LinkedHashMap<Item, ItemStack> ingredients = collectIngredients();
            for (Node child : children) {
                if (child instanceof WorkbenchNode)
                    continue;
                int inputAmount = ingredients.get(child.target).getCount();
                int outputAmount = ((RecipeCraftingProcess) processes.get(0)).recipe.getOutput().getCount();
                int maxCraftable = state.naiveMaxCraftable.get(child);
                outputFactor = Math.min(outputFactor, outputAmount * (maxCraftable / inputAmount));
            }
            state.naiveMaxCraftable.put(this, outputFactor);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            LinkedHashMap<Item, ItemStack> stackTypes = new LinkedHashMap<>();
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
            LinkedHashMap<Item, ItemStack> collected = collectIngredients();
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
        protected int hashCodeInternal(int hash) {
            return 31 * hash + ((RecipeCraftingProcess) processes.get(0)).recipe.hashCode();
        }
        @Override
        protected int equivalenceClassHashCodeInternal(int hash) {
            return 31 * hash + ((RecipeCraftingProcess) processes.get(0)).recipe.hashCode();
        }
        @Override
        protected boolean equalsInternal(Object other) {
            if (other instanceof RecipeNode)
                return ((RecipeCraftingProcess) processes.get(0)).recipe.equals(((RecipeCraftingProcess) ((Node) other).processes.get(0)).recipe);
            return false;
        }
        @Override
        protected boolean equivalenceClassEqualsInternal(Object other) {
            if (other instanceof RecipeNode)
                return ((RecipeCraftingProcess) processes.get(0)).recipe.equals(((RecipeCraftingProcess) ((Node) other).processes.get(0)).recipe);
            return false;
        }
        @Override
        protected Node cloneInternal() {
            return new RecipeNode(target, processes);
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
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
            int neededToCraft = getNeededToCraft(state.neededMap.getOrDefault(nodeId, 0));
            int craftingOutput = 0;
            while ((craftingOutput = calculateCraftingOutput()) <= neededToCraft && craftingOutput > 0) {
                adjustTotalAvailability(((RecipeCraftingProcess)processes.get(0)).recipe, craftingOutput);
                if (!usingCraftingTable() && !usingInventory()) return false;
                arrangeRecipe(((RecipeCraftingProcess) processes.get(0)).recipe, craftingOutput / ((RecipeCraftingProcess) processes.get(0)).recipe.getOutput().getCount(), useInventory);
                if (!awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false, false))
                    return false;
                if (!usingCraftingTable() && !usingInventory()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false, false);
                neededToCraft -= craftingOutput;
            }
            for (int i = 0; i < neededToCraft / ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(); i++) {
                adjustTotalAvailability(((RecipeCraftingProcess)processes.get(0)).recipe, ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount());
                if (!usingCraftingTable() && !usingInventory()) return false;
                arrangeRecipe(((RecipeCraftingProcess) processes.get(0)).recipe, 1, useInventory);
                if (!awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false, false))
                    return false;
                if (!usingCraftingTable() && !usingInventory()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false, false);
            }
            //containerManager.closeScreen();
            return true;
        }
    }

    public class ChoiceNode extends Node {
        public ChoiceNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected boolean shouldPruneNow(LinkedHashSet<Item> visited, LinkedHashSet<Item> resetVisited) {
            if (toolCraftingManager.tools.contains(target)) {
                if (!toolCraftingManager.isEssentialTool(target)) {
                    for (Item item : resetVisited) {
                        if (toolCraftingManager.tools.contains(item))
                            return true;
                    }
                }
                Set<Item> inferiorTools = toolCraftingManager.getInferiorTools(target);
                for (Item tool : inferiorTools) {
                    if (resetVisited.contains(tool))
                        return true;
                }
            }
            return false;
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
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
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            List<Node> prioritizedChildren = new ArrayList<>(children);
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
            }
            prioritizedChildren.sort(Comparator.comparingDouble(c -> state.efficiencyEquations.get(c).singleProductionCost()));
            for (Node child : prioritizedChildren) {
                res = res.concat(state.efficiencyEquations.get(child));
            }
            return res;
        }
        @Override
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            int toConsume = 0;
            if (resources.containsKey(nodeId))
                toConsume = resources.get(nodeId).getLeft().getValue() - excessOverflow;
            List<Integer> order = IntStream.range(0, children.size()).boxed().collect(Collectors.toList());
            if (resources.executionOrder.containsKey(nodeId))
                order = resources.executionOrder.get(nodeId);
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < order.size(); i++) {
                int index = order.get(i);
                long childId = nodeId + childIds.get(index);
                Node child = children.get(index);
                if (toConsume == 0)
                    break;
                int numNeeded = 0;
                if (resources.containsKey(childId))
                    numNeeded = resources.get(childId).getLeft().getValue();
                int overflow = Math.max(0, numNeeded - toConsume);
                numNeeded = Math.min(toConsume, numNeeded);
                toConsume -= numNeeded;
                if (!child.consumeResources(childId, resources, state, overflow, visited))
                    return false;
            }
            return true;
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    Pair<OperableInteger, ResourceDomain> childItem = src.get(childId);
                    child.stackResources(childId, state, (childItem.getLeft().getValue() * num) / item.getLeft().getValue(), dest, src, visited);
                }
            }
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(long nodeId, int numNeeded, int actualNeeded, CraftingState state, boolean useHeuristic, LinkedHashSet<Item> visited) {
            Resources<OperableInteger> res = new Resources<>();
            int originalNumNeeded = numNeeded;
            List<Pair<Node, Pair<Long, Integer>>> options = new ArrayList<>();
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (state.naiveMaxCraftable.get(child) > 0)
                    options.add(new Pair<>(child, new Pair<>(childId, i)));
            }
            Collections.sort(options, Comparator.comparing(option -> state.efficiencyEquations.get(option.getLeft()).evaluate(originalNumNeeded)));
            if (options.size() == 0)
                return new Pair<>(actualNeeded == 0, res);
            List<Integer> executionOrder = new ArrayList<>();
            for (Pair<Node, Pair<Long, Integer>> item : options) {
                executionOrder.add(item.getRight().getRight());
            }
            res.executionOrder.put(nodeId, executionOrder);
            CraftingState newState = state.clone();
            Pair<Boolean, Resources<OperableInteger>> childRes = options.get(0).getLeft().getBaseResources(options.get(0).getRight().getLeft(), 1, Math.min(1, actualNeeded), newState.clone(), useHeuristic, visited);
            int amount = 0;
            int actualAmount = 0;
            while (numNeeded > 0 && options.size() > 0) {
                if (!childRes.getLeft()) {
                    options.remove(0);
                    if (options.size() == 0) {
                        break;
                    }
                    childRes = options.get(0).getLeft().getBaseResources(options.get(0).getRight().getLeft(), 1, Math.min(1, actualNeeded), newState.clone(), useHeuristic, visited);
                    continue;
                }
                if (actualNeeded == 0 || options.get(0).getLeft().consumeResources(options.get(0).getRight().getLeft(), childRes.getRight(), newState, 0, visited)) {
                    numNeeded--;
                    if (actualNeeded > 0)
                        actualAmount++;
                    actualNeeded = Math.max(0, actualNeeded - 1);
                    amount++;
                    if (amount > 0 && numNeeded == 0) {
                        Resources<OperableInteger> toConsume = options.get(0).getLeft().stackResourcesBase(options.get(0).getRight().getLeft(), state, amount, childRes.getRight(), visited);
                        if (actualAmount > 0) {
                            Resources<OperableInteger> actualToConsume = toConsume;
                            if (actualAmount != amount)
                                actualToConsume = options.get(0).getLeft().stackResourcesBase(options.get(0).getRight().getLeft(), state, actualAmount, childRes.getRight(), visited);
                            options.get(0).getLeft().consumeResources(options.get(0).getRight().getLeft(), actualToConsume, state, 0, visited);
                        }
                        mergeResources(res, toConsume);
                    }
                }
                else {
                    if (amount > 0) {
                        Resources<OperableInteger> toConsume = options.get(0).getLeft().stackResourcesBase(options.get(0).getRight().getLeft(), state, amount, childRes.getRight(), visited);
                        if (actualAmount > 0) {
                            Resources<OperableInteger> actualToConsume = toConsume;
                            if (actualAmount != amount)
                                actualToConsume = options.get(0).getLeft().stackResourcesBase(options.get(0).getRight().getLeft(), state, actualAmount, childRes.getRight(), visited);
                            options.get(0).getLeft().consumeResources(options.get(0).getRight().getLeft(), actualToConsume, state, 0, visited);
                        }
                        mergeResources(res, toConsume);
                    }
                    amount = 0;
                    actualAmount = 0;
                    newState = state.clone();
                    childRes = options.get(0).getLeft().getBaseResources(options.get(0).getRight().getLeft(), 1, Math.min(1, actualNeeded), state.clone(), useHeuristic, visited);
                }
            }
            res.put(nodeId, new Pair<>(new OperableInteger(originalNumNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(actualNeeded == 0, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            for (Node child : children) {
                state.naiveMaxCraftable.put(this, state.naiveMaxCraftable.getOrDefault(this, 0) + state.naiveMaxCraftable.get(child));
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
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        /*@Override
        protected int hashCodeInternal(int hash) {
            if (toolCraftingManager.tools.contains(target))
                return 31 * hash + resetVisited.hashCode();
            return hash;
        }
        @Override
        protected boolean equalsInternal(Object other) {
            if (other instanceof ChoiceNode && toolCraftingManager.tools.contains(target) && toolCraftingManager.tools.contains(((ChoiceNode) other).target))
                return resetVisited.equals(((ChoiceNode) other).resetVisited);
            return false;
        }*/
        @Override
        protected Node cloneInternal() {
            return new ChoiceNode(target, processes);
        }
    }

    public class Resources<T extends ArithmeticOperable> {
        private LinkedHashMap<Long, List<Integer>> executionOrder;
        private LinkedHashMap<Long, Pair<T, ResourceDomain>> resources;
        public Resources() {
            executionOrder = new LinkedHashMap<>();
            resources = new LinkedHashMap<>();
        }
        public void put(long nodeId, Pair<T, ResourceDomain> res) {
            resources.put(nodeId, res);
        }
        public Pair<T, ResourceDomain> get(long nodeId) {
            return resources.get(nodeId);
        }
        public Pair<T, ResourceDomain> getOrDefault(long nodeId, Pair<T, ResourceDomain> def) {
            return resources.getOrDefault(nodeId, def);
        }
        public boolean containsKey(long nodeId) {
            return resources.containsKey(nodeId);
        }
        public Set<Long> keySet() {
            return resources.keySet();
        }
        public Resources<T> mult(int num) {
            Resources<T> res = new Resources<>();
            for (Long item : resources.keySet()) {
                Pair<T, ResourceDomain> value = resources.get(item);
                res.put(item, new Pair<>((T) value.getLeft().mult(num), value.getRight()));
            }
            return res;
        }
        public Resources<OperableInteger> ceil() {
            Resources<OperableInteger> res = new Resources<>();
            for (Long item : resources.keySet()) {
                Pair<T, ResourceDomain> value = resources.get(item);
                res.put(item, new Pair<>(new OperableInteger(value.getLeft().ceil()), value.getRight()));
            }
            return res;
        }
        public Resources<T> clone() {
            Resources<T> res = new Resources<>();
            for (Long item : resources.keySet()) {
                Pair<T, ResourceDomain> value = resources.get(item);
                res.put(item, new Pair<>((T) value.getLeft().clone(), value.getRight()));
            }
            for (Long item : executionOrder.keySet()) {
                List<Integer> value = executionOrder.get(item);
                res.executionOrder.put(item, new ArrayList<>(value));
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

    public class OperableInteger implements ArithmeticOperable<OperableInteger> {
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

    public class Fraction implements ArithmeticOperable<Fraction> {
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

    public class InventoryNode extends Node {
        public InventoryNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.inventoryAvailability.getOrDefault(target, 0) > 0;
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            return new EfficiencyEquation(List.of(new EfficiencyEquationSegment(0, 0, 0, state.naiveMaxCraftable.get(this))));
        }
        @Override
        protected ResourceDomain getResourceDomain() {
            return ResourceDomain.INVENTORY;
        }
        @Override
        protected boolean drawResourcesFromState(CraftingState state, int numNeeded, int actualNeeded) {
            state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) - actualNeeded);
            return state.inventoryAvailability.get(target) >= 0;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, state.inventoryAvailability.getOrDefault(target, 0));
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            return inventoryQuery.acquire(target, target, state.neededMap.getOrDefault(nodeId, 0));
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new InventoryNode(target, processes);
        }
    }

    public class StorageNode extends Node {
        public StorageNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.storageAvailability.getOrDefault(target, 0) > 0;
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            double base = containerQuery.getDistanceToContainer(target) / MC.player.getMovementSpeed();
            return new EfficiencyEquation(List.of(new EfficiencyEquationSegment(base, 0, 0, state.naiveMaxCraftable.get(this))));
        }
        @Override
        protected ResourceDomain getResourceDomain() {
            return ResourceDomain.STORAGE;
        }
        @Override
        protected boolean drawResourcesFromState(CraftingState state, int numNeeded, int actualNeeded) {
            state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) - actualNeeded);
            return state.storageAvailability.get(target) >= 0;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, state.storageAvailability.getOrDefault(target, 0));
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            return containerQuery.acquire(target, target, state.neededMap.getOrDefault(nodeId, 0));
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new StorageNode(target, processes);
        }
    }

    public class WorldNode extends Node {
        private Item dropped;
        private Block block;
        public WorldNode(Item dropped, Block block, List<CraftingProcess> processes) {
            super(dropped, processes);
            this.dropped = dropped;
            this.block = block;
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    child.stackResources(childId, state, 1, dest, src, visited);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            Block block = ((WorldCraftingProcess) processes.get(0)).block;
            return List.of(new ToolCraftingProcess(block).getNode());
        }
        @Override
        protected boolean canPossiblyCraft(CraftingState state) {
            return state.worldAvailability.getOrDefault(((WorldCraftingProcess) processes.get(0)).block, 0) > 0;
        }
        @Override
        protected ResourceDomain getResourceDomain() {
            return ResourceDomain.WORLD;
        }
        @Override
        protected boolean drawResourcesFromState(CraftingState state, int numNeeded, int actualNeeded) {
            state.worldAvailability.put(block, state.worldAvailability.getOrDefault(block, 0) - actualNeeded);
            return state.worldAvailability.get(block) >= 0;
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            Block targetBlock = ((WorldCraftingProcess) processes.get(0)).block;
            double base = getNearestBlockDistance(targetBlock, 0.0) / MC.player.getMovementSpeed();
            EfficiencyEquation eq = new EfficiencyEquation(List.of(new EfficiencyEquationSegment(base, 0, 0, state.naiveMaxCraftable.get(this))));
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, 1);
                eq = eq.add(state.efficiencyEquations.get(child));
            }
            return eq;
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(long nodeId, int numNeeded, int actualNeeded, CraftingState state, boolean useHeuristic, LinkedHashSet<Item> visited) {
            Resources<OperableInteger> res = new Resources<>();
            if (!getActivePathfinder().isMiningSupported())
                return new Pair<>(false, res);
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.WORLD));
            state.worldAvailability.put(block, state.worldAvailability.getOrDefault(block, 0) - actualNeeded);
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(childId, numNeeded, actualNeeded, state, useHeuristic, visited);
                if (!childRes.getLeft())
                    return new Pair<>(false, res);
                mergeResources(res, childRes.getRight());
            }
            return new Pair<>(state.worldAvailability.get(block) >= 0, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, state.worldAvailability.getOrDefault(block, 0));
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            if (!block.getDefaultState().isToolRequired())
                return worldQuery.acquire(block, dropped, state.neededMap.getOrDefault(nodeId, 0));
            if (!toolCraftingManager.canMine(block))
                return false;
            return worldQuery.acquire(block, dropped, state.neededMap.getOrDefault(nodeId, 0));
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected int hashCodeInternal(int hash) {
            return 31 * hash + block.hashCode();
        }
        @Override
        protected int equivalenceClassHashCodeInternal(int hash) {
            return 31 * hash + block.hashCode();
        }
        @Override
        protected boolean equalsInternal(Object other) {
            if (other instanceof WorldNode) {
                return block.equals(((WorldNode)other).block);
            }
            return false;
        }
        @Override
        protected boolean equivalenceClassEqualsInternal(Object other) {
            if (other instanceof WorldNode) {
                return block.equals(((WorldNode)other).block);
            }
            return false;
        }
        @Override
        protected Node cloneInternal() {
            return new WorldNode(dropped, block, processes);
        }
    }

    public class PathingNode extends Node {
        public PathingNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            return new ArrayList<>();
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            double base = getNearestBlockDistance(targetBlock, 0.0) / MC.player.getMovementSpeed();
            return new EfficiencyEquation(List.of(new EfficiencyEquationSegment(base, 0, 0, 1)));
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            state.naiveMaxCraftable.put(this, state.worldAvailability.getOrDefault(targetBlock, 0));
        }
        @Override
        protected boolean shouldAlwaysConsiderPossible() {
            return true;
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            //Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            //return pathFinder.path(nearestBlockPosMap.getOrDefault(targetBlock, BlockPos.ORIGIN));
            Block targetBlock = ((PathingCraftingProcess) processes.get(0)).block;
            BlockPos nearestTarget = containerManager.getClosestToPlayer(targetBlock);
            if (nearestTarget != null)
                return containerManager.navigateAndOpenContainer(nearestTarget);
            if (!nearestBlockPosMap.containsKey(targetBlock))
                return false;
            nearestTarget = nearestBlockPosMap.get(targetBlock).up();
            return pathTo(nearestTarget);
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new PathingNode(target, processes);
        }
    }

    public class PlacementNode extends Node {
        public PlacementNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    child.stackResources(childId, state, 1, dest, src, visited);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            CraftingPlan plan = getCraftingPlan(Registry.ITEM.getId(target).getPath());
            List<CraftingProcess> processes = new ArrayList<>();
            for (CraftingProcess process : plan.processes) {
                if (!(process instanceof WorldCraftingProcess)) {
                    processes.add(process);
                }
            }
            return List.of(new ChoiceNode(target, processes).setDistinction(getClass()));
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation(List.of(new EfficiencyEquationSegment(0.1, 0, 0, state.naiveMaxCraftable.get(this))));
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
                res = res.add(state.efficiencyEquations.get(child));
            }
            return res;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, 0);
            for (Node child : children) {
                state.naiveMaxCraftable.put(this, state.naiveMaxCraftable.get(child));
            }
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            BlockPos pos =  blockManager.getNearestPlaceablePosition();
            blockManager.placeBlock(target, pos);
            Block block = ((PlacementCraftingProcess) processes.get(0)).block;
            containerManager.addContainer(block, pos);
            state.worldAvailability.put(block, state.worldAvailability.getOrDefault(block, 0) + 1);
            blockTree.addBlock(block, pos);
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new PlacementNode(target, processes);
        }
    }

    public class WorkbenchNode extends Node {
        public WorkbenchNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
            setShouldOnlyCraftOne(true);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    child.stackResources(childId, state, 1, dest, src, visited);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            List<CraftingProcess> processes = new ArrayList<>();
            processes.add(new PathingCraftingProcess(((WorkbenchCraftingProcess) this.processes.get(0)).block));
            processes.add(new PlacementCraftingProcess(((WorkbenchCraftingProcess) this.processes.get(0)).block));
            return List.of(new ChoiceNode(target, processes).setDistinction(getClass()));
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
                res = res.add(state.efficiencyEquations.get(child));
            }
            return res;
        }
        @Override
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            //Pair<OperableInteger, ResourceDomain> res = resources.getOrDefault(nodeId, new Pair<>(new OperableInteger(), ResourceDomain.COMPOSITE));
            //res.setLeft(new OperableInteger());
            //resources.put(nodeId, res);
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (!child.consumeResources(childId, resources, state, excessOverflow, visited))
                    return false;
            }
            return true;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, 0);
            for (Node child : children) {
                state.naiveMaxCraftable.put(this, state.naiveMaxCraftable.get(child));
            }
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new WorkbenchNode(target, processes);
        }
    }

    public class ToolNode extends Node {
        private LinkedHashMap<Node, EfficiencyEquation> toolEfficiencyEquations;
        private EfficiencyEquation handEfficiencyEquation;
        public ToolNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
            setShouldPruneTarget(false);
            toolEfficiencyEquations = new LinkedHashMap<>();
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    child.stackResources(childId, state, 1, dest, src, visited);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            Set<Item> matchingTools = toolCraftingManager.getMatchingTools(((ToolCraftingProcess) processes.get(0)).block);
            List<Node> toolNodes = new ArrayList<>();
            for (Item tool : matchingTools) {
                CraftingPlan plan = getCraftingPlan(Registry.ITEM.getId(tool).getPath());
                toolNodes.add(plan.getNode());
            }
            for (Node node : toolNodes) {
                node.setDistinction(getClass()).setShouldOnlyCraftOne(true).setShouldResetVisited(true);
            }
            return toolNodes;
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            Block block = ((ToolCraftingProcess) processes.get(0)).block;
            int blockAvailability = state.worldAvailability.getOrDefault(block, 0);
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
                double base = state.efficiencyEquations.get(child).evaluate(1);
                double coefficient = blockBreakingTime(block, (ToolItem)child.target) * 30;
                EfficiencyEquation eq = new EfficiencyEquation(List.of(new EfficiencyEquationSegment(base, coefficient, 0, blockAvailability)));
                toolEfficiencyEquations.put(child, eq);
                res = res.min(eq);
            }
            handEfficiencyEquation = new EfficiencyEquation(List.of(new EfficiencyEquationSegment(0, blockBreakingTime(block, null) * 30, 0, blockAvailability)));
            if (!block.getDefaultState().isToolRequired())
                res = res.min(handEfficiencyEquation);
            return res;
        }
        @Override
        protected boolean consumeResourcesInternal(long nodeId, Resources<OperableInteger> resources, CraftingState state, int excessOverflow, LinkedHashSet<Item> visited) {
            List<Integer> order = IntStream.range(0, children.size()).boxed().collect(Collectors.toList());
            if (resources.executionOrder.containsKey(nodeId))
                order = resources.executionOrder.get(nodeId);
            List<Long> childIds = state.children.getOrDefault(this, new ArrayList<>());
            for (int i = 0; i < order.size(); i++) {
                int index = order.get(i);
                long childId = nodeId + childIds.get(index);
                Node child = children.get(index);
                if (!child.consumeResources(childId, resources, state, excessOverflow, visited))
                    return false;
            }
            return true;
        }
        @Override
        protected Pair<Boolean, Resources<OperableInteger>> getBaseResourcesInternal(long nodeId, int numNeeded, int actualNeeded, CraftingState state, boolean useHeuristic, LinkedHashSet<Item> visited) {
            Resources<OperableInteger> res = new Resources<>();
            Set<Item> possibleTools = toolCraftingManager.getMatchingTools(((ToolCraftingProcess) processes.get(0)).block);
            for (Item item : totalInventoryAvailabilityMap.keySet()) {
                if (possibleTools.contains(item)) {
                    return new Pair<>(true, res);
                }
            }
            List<Long> childIds = state.children.get(this);
            List<Pair<Node, Pair<Long, Integer>>> options = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (state.naiveMaxCraftable.getOrDefault(child, 0) > 0)
                    options.add(new Pair<>(child, new Pair<>(childId, i)));
            }
            LinkedHashMap<Node, Double> efficiency = new LinkedHashMap<>();
            for (Pair<Node, Pair<Long, Integer>> option : options) {
                efficiency.put(option.getLeft(), toolEfficiencyEquations.get(option.getLeft()).evaluate(numNeeded));
            }
            options.sort(Comparator.comparingDouble(x -> efficiency.get(x.getLeft())));
            List<Integer> executionOrder = new ArrayList<>();
            for (Pair<Node, Pair<Long, Integer>> item : options) {
                executionOrder.add(item.getRight().getRight());
            }
            res.executionOrder.put(nodeId, executionOrder);
            double handEfficiency = handEfficiencyEquation.evaluate(numNeeded);
            boolean toolRequired = ((ToolCraftingProcess) processes.get(0)).block.getDefaultState().isToolRequired();
            boolean foundCompatibleTool = false;
            for (Pair<Node, Pair<Long, Integer>> option : options) {
                long childId = option.getRight().getLeft();
                Node child = option.getLeft();
                if (!toolRequired && efficiency.get(child) >= handEfficiency)
                    break;
                Pair<Boolean, Resources<OperableInteger>> childRes = child.getBaseResources(childId, Math.min(1, numNeeded), Math.min(1, actualNeeded), state, useHeuristic, visited);
                if (childRes.getLeft()) {
                    foundCompatibleTool = true;
                    mergeResources(res, childRes.getRight());
                    break;
                }
            }
            if (toolRequired && !foundCompatibleTool)
                return new Pair<>(false, res);
            res.put(nodeId, new Pair<>(new OperableInteger(numNeeded), ResourceDomain.COMPOSITE));
            return new Pair<>(true, res);
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, 0);
            int childrenCraftable = 0;
            for (Node child : children) {
                childrenCraftable += state.naiveMaxCraftable.get(child);
            }
            if (childrenCraftable > 0 || !requiresAnyChildren())
                state.naiveMaxCraftable.put(this, state.worldAvailability.getOrDefault(((ToolCraftingProcess) processes.get(0)).block, 0));
        }
        @Override
        protected boolean requiresAllChildren() {
            return false;
        }
        @Override
        protected boolean requiresAnyChildren() {
            return ((ToolCraftingProcess) processes.get(0)).block.getDefaultState().isToolRequired();
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new ToolNode(target, processes);
        }
    }

    public class GroupNode extends Node {
        public GroupNode(Item target, List<CraftingProcess> processes) {
            super(target, processes);
            setShouldPruneTarget(false);
        }
        @Override
        protected void stackResourcesInternal(long nodeId, CraftingState state, int num, Resources<OperableInteger> dest, Resources<OperableInteger> src, LinkedHashSet<Item> visited) {
            Pair<OperableInteger, ResourceDomain> item = src.get(nodeId);
            dest.put(nodeId, new Pair<>(new OperableInteger(num), item.getRight()));
            List<Long> childIds = state.children.get(this);
            for (int i = 0; i < children.size(); i++) {
                long childId = nodeId + childIds.get(i);
                Node child = children.get(i);
                if (src.containsKey(childId)) {
                    child.stackResources(childId, state, num, dest, src, visited);
                }
            }
        }
        @Override
        protected List<Node> getChildrenInternal(LinkedHashSet<Item> nodes) {
            List<Item> group = ((GroupCraftingProcess) processes.get(0)).items;
            List<CraftingProcess> itemProcesses = new ArrayList<>();
            for (Item member : group) {
                CraftingPlan plan = getCraftingPlan(Registry.ITEM.getId(member).getPath());
                itemProcesses.addAll(plan.processes);
            }
            return List.of(new ChoiceNode(target, itemProcesses).setDistinction(getClass()).setShouldPruneTarget(false));
        }
        @Override
        protected EfficiencyEquation genEfficiencyEquationsInternal(CraftingState state, LinkedHashSet<Item> visited, int numNeeded) {
            EfficiencyEquation res = new EfficiencyEquation();
            for (Node child : children) {
                child.genEfficiencyEquations(state, visited, numNeeded);
                res = res.add(state.efficiencyEquations.get(child));
            }
            return res;
        }
        @Override
        protected void genNaiveMaxCraftableInternal(CraftingState state) {
            state.naiveMaxCraftable.put(this, 0);
            for (Node child : children) {
                state.naiveMaxCraftable.put(this, state.naiveMaxCraftable.get(child));
            }
        }
        @Override
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        public LinkedHashMap<Item, ItemStack> collectIngredients() {
            return new LinkedHashMap<>();
        }
        @Override
        public boolean execute(long nodeId, CraftingState state) {
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
        @Override
        protected Node cloneInternal() {
            return new GroupNode(target, processes);
        }
    }

    private Pathfinder getActivePathfinder() {
        if (baritoneChatInterfaceEnabled.isChecked())
            return baritoneChatPathfinder;
        return wurstPathfinder;
    }

    private boolean usingCraftingTable() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof CraftingScreenHandler;
    }

    private boolean usingFurnace() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof FurnaceScreenHandler;
    }

    private boolean usingSmithingTable() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof SmithingScreenHandler;
    }

    private boolean usingInventory() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof PlayerScreenHandler;
    }

    private Node makeRootNode(String identifier) {
        return getCraftingPlan(identifier).getNode();
    }

    public class CraftingQueueEntry {
        private String itemId;
        private int count;
        private boolean craftAll;
        public CraftingQueueEntry(String itemId, int count, boolean craftAll) {
            this.itemId = itemId;
            this.count = count;
            this.craftAll = craftAll;
        }
    }

    private enum ResourceDomain {
        INVENTORY, STORAGE, WORLD, COMPOSITE
    }

    public void queueCraft(String itemId, int count, boolean craftAll) {
        synchronized(craftingQueue) {
            craftingQueue.add(new CraftingQueueEntry(itemId, count, craftAll));
        }
    }

    private void updateTotalInventoryAvailability() {
        MCRunnable updateRunnable = new MCRunnable() {
            @Override
            protected void runInternal() {
                synchronized (totalInventoryAvailabilityMap) {
                    totalInventoryAvailabilityMap = inventoryQuery.getAvailabilityMap();
                }
            }
        };
        updateRunnable.runUntilDone();
    }

    private void initSlotUpdates() {
        latestSlotUpdates = new LinkedHashMap<>();
    }

    public void notifySlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
        if (packet.getSlot() != 0) {
            if (packet.getRevision() == lastSlotUpdatePacketRevision)
                return;
            if (packet.getSyncId() == ScreenHandlerSlotUpdateS2CPacket.UPDATE_CURSOR_SYNC_ID || packet.getSyncId() == ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID)
                return;
            lastSlotUpdatePacketRevision = packet.getRevision();
            MCRunnable updateRunnable = new MCRunnable() {
                protected void runInternal() {
                    synchronized (totalInventoryAvailabilityMap) {
                        totalInventoryAvailabilityMap.clear();
                        totalInventoryAvailabilityMap.putAll(inventoryQuery.getScreenAvailabilityMap());
                    }
                }
            };
            updateRunnable.runWithoutWaiting();
        }
        slotUpdateLock.lock();
        try {
            latestSlotUpdates.put(packet.getSlot(), new SlotUpdateInfo(packet.getSlot(), packet.getItemStack()));
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
            LinkedHashMap<Item, Integer> contentMap = new LinkedHashMap<>();
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

    private void craft(String itemId, int count, boolean craftAll) {
        isCurrentlyCrafting = true;
        new Thread(() -> {
            generateBlockMetrics();
            containerManager.updateContainers(worldQuery.getLocations(containerBlockTypes));
            Node root = makeRootNode(itemId);
            Item item = root.target;
            int initialCount = 0;
            if (root.shouldPruneTarget) {
                initialCount = inventoryAvailabilityMap.getOrDefault(item, 0);
                inventoryAvailabilityMap.put(item, 0);
            }
            root.craft(craftAll ? Integer.MAX_VALUE : count);
            if (root.shouldPruneTarget)
                inventoryAvailabilityMap.put(item, inventoryAvailabilityMap.getOrDefault(item, 0) + initialCount);
            /*Item item = Registry.ITEM.get(itemId);
            int initialCount = inventoryAvailabilityMap.getOrDefault(item, 0);
            inventoryAvailabilityMap.put(item, 0);
            if (craftAll) {
                root = makeRootNode(itemId, getMaxCraftable(itemId, CraftingParams.VERIFY_AND_GENERATE, new CraftingState(inventoryAvailabilityMap, storageAvailabilityMap, worldAvailabilityMap, new LinkedHashSet<>())));
            }
            CraftingState verificationState = root.verify();
            if (verificationState.success) {
                LinkedHashMap<Item, Integer> obtainFromStorage = new LinkedHashMap<>();
                for (StorageNode node : verificationState.storageNodes) {
                    obtainFromStorage.put(node.target, obtainFromStorage.getOrDefault(node.target, 0) + node.needed);
                    storageAvailabilityMap.put(node.target, storageAvailabilityMap.getOrDefault(node.target, 0) - node.needed);
                }
                LinkedHashMap<Item, Integer> original = (LinkedHashMap<Item, Integer>) obtainFromStorage.clone();
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
        baritoneChatInterface = new BaritoneChatInterface();
        baritoneChatPathfinder = new BaritoneChatPathfinder();
        wurstPathfinder = new WurstPathfinder();
        if (groups == null)
            initGroups();
        if (processMap == null)
            initProcessMap();
        if (containerBlockTypes == null)
            initContainerBlockTypes();
        EVENTS.add(UpdateListener.class, this);
    }

    public BlockPos getNearestBlockPos(Block block) {
        return nearestBlockPosMap.computeIfAbsent(block, k -> {
            Vec3d playerPos = MC.player.getPos();
            BlockPos playerBlockPos = new BlockPos((int)playerPos.getX(), (int)playerPos.getY(), (int)playerPos.getZ());
            return blockTree.findClosest(block, playerBlockPos);
        });
    }

    public double getNearestBlockDistance(Block block, double defaultValue) {
        BlockPos pos = getNearestBlockPos(block);
        if (pos == null)
            return defaultValue;
        return nearestBlockDistanceMap.computeIfAbsent(block, k -> {
            Vec3d vecPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d playerPos = MC.player.getPos();
            return vecPos.subtract(playerPos).length();
        });
    }

    public void clearNearestBlockMetrics() {
        nearestBlockPosMap.clear();
        nearestBlockDistanceMap.clear();
    }

    public void generateBlockMetrics() {
        Vec3d playerPos = MC.player.getPos();
        BlockPos playerBlockPos = new BlockPos((int)playerPos.getX(), (int)playerPos.getY(), (int)playerPos.getZ());
        if (blockTree == null || !blockTree.containsPointXZ(playerBlockPos)) {
            blockTree = createBlockTree();
            nearestBlockPosMap = new LinkedHashMap<>();
            nearestBlockDistanceMap = new LinkedHashMap<>();
        }
    }

    @Override
    public void onUpdate() {
        synchronized (runnableQueue) {
            for (WurstRunnable runnable : runnableQueue) {
                runnable.run();
            }
            runnableQueue.clear();
        }
        synchronized (craftingQueue) {
            if (!isCurrentlyCrafting) {
                if (craftingQueue.size() > 0) {
                    CraftingQueueEntry entry = craftingQueue.get(0);
                    if (doneCrafting) {
                        synchronized (totalInventoryAvailabilityMap) {
                            totalInventoryAvailabilityMap = inventoryQuery.getAvailabilityMap();
                            inventoryAvailabilityMap = (LinkedHashMap<Item, Integer>) totalInventoryAvailabilityMap.clone();
                        }
                        storageAvailabilityMap = containerQuery.getAvailabilityMap();
                        worldAvailabilityMap = worldQuery.getAvailabilityMap();
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
