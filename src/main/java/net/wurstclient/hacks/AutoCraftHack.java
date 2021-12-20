package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
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
    private HashMap<Item, Integer> totalInventoryAvailabilityMap = new HashMap<>();

    private HashSet<Block> containerBlockTypes;

    private Pathfinder pathFinder = new WurstPathfinder();

    private BlockPos latestBlockPos = BlockPos.ORIGIN;

    private boolean doneCrafting = true;

    private ContainerManager containerManager = new ContainerManager();

    public AutoCraftHack() {
        super("AutoCraft");
        setCategory(Category.ITEMS);
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
    }

    private List<CraftingProcess> getProcesses(Identifier id) {
        List<CraftingProcess> processes = new ArrayList<>(processMap.getOrDefault(id, new ArrayList<>()));
        processes.add(new StorageCraftingProcess(Registry.ITEM.get(id)));
        return processes;
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
        public abstract void acquire(T item, int count);
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
        public void acquire(Item item, int count) { }
    }

    private abstract class Pathfinder {
        public abstract void path(BlockPos pos);
    }

    private class WurstPathfinder extends Pathfinder {
        public void path(BlockPos pos) {
            GoToCmd path = new GoToCmd();
            path.setGoal(pos);
            path.enable();
            path.waitUntilDone();
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
                        totalInventoryAvailabilityMap.put(item, totalInventoryAvailabilityMap.getOrDefault(item, 0) + handler.getSlot(i).getStack().getCount());
                        MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, MC.player);
                    }
                }
            }
            container.put(item, container.getOrDefault(item, 0) - (initialCount - count));
            return count;
        }
        @Override
        public void acquire(Item item, int count) {
            for (BlockPos pos : containers.keySet()) {
                if (count <= 0)
                    break;
                if (containers.get(pos).getOrDefault(item, 0) > 0) {
                    containerManager.goToContainer(pos);
                    count = takeItem(pos, item, count);
                }
            }
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
        public void acquire(Block block, int count) { }
    }

    private abstract class CraftingProcess {
        public abstract int getMultiplicity();
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
    }

    private class ContainerManager {
        private HashMap<Block, HashSet<BlockPos>> containers = new HashMap<>();
        private BlockPos currentContainer = null;
        public void goToContainer(BlockPos container) {
            if (container.equals(currentContainer))
                return;
            if (MC.currentScreen != null)
                closeScreen();
            pathFinder.path(container.up());
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

    private class CraftingState {
        private HashMap<Item, Integer> inventoryAvailability;
        private HashMap<Item, Integer> storageAvailability;
        private HashSet<Node> visited;
        private boolean collectStorageNodes = false;
        private List<StorageNode> storageNodes = new ArrayList<>();
        private boolean success = false;
        public CraftingState(HashMap<Item, Integer> inventoryAvailability, HashMap<Item, Integer> storageAvailability, HashSet<Node> visited) {
            this.inventoryAvailability = inventoryAvailability;
            this.storageAvailability = storageAvailability;
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
        public CraftingState setCollectStorageNodes(boolean value) {
            collectStorageNodes = value;
            return this;
        }
        public CraftingState clone() {
            CraftingState state = new CraftingState((HashMap<Item, Integer>) inventoryAvailability.clone(), (HashMap<Item, Integer>) storageAvailability.clone(), (HashSet<Node>) visited.clone());
            state.success = success;
            state.collectStorageNodes = collectStorageNodes;
            state.storageNodes = new ArrayList<>(storageNodes);
            return state;
        }
        public void set(CraftingState other) {
            inventoryAvailability.clear();
            inventoryAvailability.putAll(other.inventoryAvailability);
            storageAvailability.clear();
            storageAvailability.putAll(other.storageAvailability);
            visited.clear();
            visited.addAll(other.visited);
            success = other.success;
            collectStorageNodes = other.collectStorageNodes;
            storageNodes.clear();
            storageNodes.addAll(other.storageNodes);
        }
    }

    private void awaitSlotUpdate(Item item, int amount, int slot, boolean onlyConsiderItem, boolean succeedAfterTimeout) {
        slotUpdateLock.lock();
        try {
            while (latestSlotUpdate == null || !latestSlotUpdate.itemStack.getItem().equals(item) || (!onlyConsiderItem && (latestSlotUpdate.itemStack.getCount() != amount || latestSlotUpdate.slot != slot))) {
                boolean gotSignal = slotUpdateCondition.await(1000, TimeUnit.MILLISECONDS);
                if (succeedAfterTimeout)
                    break;
                if (!gotSignal) {
                    ItemStack craftingItem = MC.player.currentScreenHandler.getSlot(0).getStack();
                    if (craftingItem.getItem().equals(item) && (onlyConsiderItem || (craftingItem.getCount() == amount && slot == 0)))
                        break;
                }
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            slotUpdateLock.unlock();
        }
    }

    private abstract class Node {
        protected Item target;
        protected int needed;
        protected List<CraftingProcess> processes;
        protected int stackShift;
        public Node(Item target, int needed, List<CraftingProcess> processes) {
            this.processes = processes;
            this.target = target;
            this.needed = needed;
            stackShift = 0;
        }
        public Node setNeeded(int needed) {
            this.needed = needed;
            return this;
        }
        public Node setStackShift(int stackShift) {
            this.stackShift = stackShift;
            return this;
        }
        public CraftingState craft() {
            HashMap<Item, Integer> inventoryAvailability = inventoryAvailabilityMap;
            HashMap<Item, Integer> storageAvailability = storageAvailabilityMap;
            HashSet<Node> visited = new HashSet<>();
            return craftPreliminary(new CraftingState(inventoryAvailability, storageAvailability, visited));
        }
        public CraftingState verify() {
            HashMap<Item, Integer> inventoryAvailability = (HashMap<Item, Integer>) inventoryAvailabilityMap.clone();
            HashMap<Item, Integer> storageAvailability = (HashMap<Item, Integer>) storageAvailabilityMap.clone();
            HashSet<Node> visited = new HashSet<>();
            return verifyPreliminary(new CraftingState(inventoryAvailability, storageAvailability, visited).setCollectStorageNodes(true));
        }
        private CraftingState craftPreliminary(CraftingState state) {
            boolean rememberVisit = shouldRememberVisit();
            if (rememberVisit)
                state.visited.add(this);
            int numAvailable = state.inventoryAvailability.getOrDefault(target, 0);
            if (numAvailable >= needed) {
                state.inventoryAvailability.put(target, numAvailable - needed);
                if (rememberVisit)
                    state.visited.remove(this);
                return state.success();
            }
            if (numAvailable > 0) {
                state.inventoryAvailability.put(target, 0);
                needed -= numAvailable;
            }
            List<Node> children = getChildren(this, state.visited);
            if (craftInternal(state, children).success) {
                if (rememberVisit)
                    state.visited.remove(this);
                return state.success();
            }
            if (rememberVisit)
                state.visited.remove(this);
            return state.setSuccess(execute());
        }
        private CraftingState verifyPreliminary(CraftingState state) {
            boolean rememberVisit = shouldRememberVisit();
            if (rememberVisit)
                state.visited.add(this);
            if (needed <= 0) {
                if (rememberVisit)
                    state.visited.remove(this);
                return state.success();
            }
            int numAvailable = state.inventoryAvailability.getOrDefault(target, 0);
            int neededOffset = 0;
            if (numAvailable >= needed) {
                state.inventoryAvailability.put(target, state.inventoryAvailability.get(target) - needed);
                if (rememberVisit)
                    state.visited.remove(this);
                return state.success();
            }
            List<Node> children = getChildren(this, state.visited);
            if (numAvailable > 0) {
                state.inventoryAvailability.put(target, 0);
                needed -= numAvailable;
                neededOffset += numAvailable;
                children = getChildren(this, state.visited);
            }
            CraftingState res = verifyInternal(state, children);
            needed += neededOffset;
            if (rememberVisit)
                state.visited.remove(this);
            return res;
        }
        protected abstract boolean shouldRememberVisit();
        protected abstract CraftingState craftInternal(CraftingState state, List<Node> children);
        protected abstract CraftingState verifyInternal(CraftingState state, List<Node> children);
        public abstract HashMap<Item, ItemStack> collectIngredients();
        public abstract boolean execute();
        public abstract int getOutputCount();
        public int getNeededToCraft(int index) {
            if (index < processes.size() && processes.get(index) instanceof RecipeCraftingProcess) {
                RecipeCraftingProcess process = (RecipeCraftingProcess)processes.get(index);
                if (needed < process.recipe.getOutput().getCount()) {
                    return process.recipe.getOutput().getCount();
                }
                else {
                    return (int)Math.ceil((double)needed / process.recipe.getOutput().getCount()) * process.recipe.getOutput().getCount();
                }
            }
            return needed;
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
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        protected CraftingState craftInternal(CraftingState state, List<Node> children) {
            if (children.size() == 0)
                return state.success();
            for (Node child : children) {
                child.craftPreliminary(state);
            }
            state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) + getNeededToCraft(0) - needed);
            return state.failure();
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
        protected CraftingState verifyInternal(CraftingState state, List<Node> children) {
            if (children.size() == 0)
                return state.failure();
            for (Node child : children) {
                if (!child.verifyPreliminary(state).success) {
                    return state.failure();
                }
            }
            state.inventoryAvailability.put(target, state.inventoryAvailability.getOrDefault(target, 0) + getNeededToCraft(0) - needed);
            return state.success();
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
                int outputFactor = Math.min(totalInventoryAvailabilityMap.getOrDefault(itemStack.getItem(), 0) / collected.get(itemStack.getItem()).getCount(), itemStack.getItem().getMaxCount()) * ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
                output = Math.min(output, outputFactor);
            }
            return output;
        }
        private void adjustTotalAvailability(Recipe<?> recipe, int craftingOutput) {
            Item outputItem = recipe.getOutput().getItem();
            totalInventoryAvailabilityMap.put(outputItem, totalInventoryAvailabilityMap.getOrDefault(outputItem, 0) + craftingOutput);
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                totalInventoryAvailabilityMap.put(stack.getItem(), totalInventoryAvailabilityMap.getOrDefault(stack.getItem(), 0) - (stack.getCount() * craftingOutput) / recipe.getOutput().getCount());
            }
        }
        @Override
        public boolean execute() {
            if (!usingCraftingTable()) {
                Block craftingTable = Registry.BLOCK.get(new Identifier("minecraft", "crafting_table"));
                BlockPos nearestCraftingTable = containerManager.getClosestToPlayer(craftingTable);
                if (nearestCraftingTable != null)
                    containerManager.goToContainer(nearestCraftingTable);
            }
            int neededToCraft = getNeededToCraft(0);
            int craftingOutput = 0;
            while ((craftingOutput = calculateCraftingOutput()) <= neededToCraft && craftingOutput > 0) {
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickRecipe(MC.player.currentScreenHandler.syncId, ((RecipeCraftingProcess)processes.get(0)).recipe, true);
                awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false);
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true, false);
                adjustTotalAvailability(((RecipeCraftingProcess)processes.get(0)).recipe, craftingOutput);
                neededToCraft -= craftingOutput;
            }
            for (int i = 0; i < neededToCraft / ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(); i++) {
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickRecipe(MC.player.currentScreenHandler.syncId, ((RecipeCraftingProcess)processes.get(0)).recipe, false);
                awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false, false);
                if (!usingCraftingTable()) return false;
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
        protected boolean shouldRememberVisit() {
            return false;
        }
        @Override
        protected CraftingState craftInternal(CraftingState state, List<Node> children) {
            for (Node child : children) {
                int original = child.needed;
                child.needed = needed;
                CraftingState verification = child.verifyPreliminary(state.clone());
                if (!verification.success) {
                    int maxCraftable = getMaxCraftable(child, state.clone());
                    if (maxCraftable > 0) {
                        child.needed = maxCraftable;
                        child.craftPreliminary(state);
                        needed -= maxCraftable;
                    }
                }
                else {
                    child.craftPreliminary(state);
                    child.needed = original;
                    return state.success();
                }
                child.needed = original;
            }
            return state.failure();
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        protected CraftingState verifyInternal(CraftingState state, List<Node> children) {
            int originalNeeded = needed;
            for (Node child : children) {
                CraftingState newState = state.clone();
                CraftingState verification = child.verifyPreliminary(newState);
                if (!verification.success) {
                    int maxCraftable = getMaxCraftable(child, state.clone());
                    int originalChildNeeded = child.needed;
                    child.needed = maxCraftable;
                    child.verifyPreliminary(state);
                    child.needed = originalChildNeeded;
                    needed -= maxCraftable;
                }
                else {
                    state.set(newState);
                    needed = originalNeeded;
                    return state.success();
                }
                if (needed <= 0) {
                    needed = originalNeeded;
                    return state.success();
                }
            }
            needed = originalNeeded;
            return state.failure();
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

    private class StorageNode extends Node {
        public StorageNode(Item target, int needed, List<CraftingProcess> processes) {
            super(target, needed, processes);
        }
        @Override
        protected boolean shouldRememberVisit() {
            return true;
        }
        @Override
        protected CraftingState craftInternal(CraftingState state, List<Node> children) {
            return state.failure();
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        protected CraftingState verifyInternal(CraftingState state, List<Node> children) {
            if (state.storageAvailability.getOrDefault(target, 0) >= needed) {
                state.storageAvailability.put(target, state.storageAvailability.getOrDefault(target, 0) - needed);
                if (state.collectStorageNodes)
                    state.storageNodes.add(this);
                return state.success();
            }
            return state.failure();
        }
        @Override
        public boolean execute() {
            containerQuery.acquire(target, needed);
            return true;
        }
        @Override
        public int getOutputCount() {
            return 1;
        }
    }

    private int gcd(int a, int b) {
        if (b == 0)
            return a;
        return gcd(b, a % b);
    }

    private class Fraction {
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
            return (int)Math.ceil((double)numerator / denominator);
        }
        @Override
        public String toString() {
            return denominator > 1 ? numerator + "/" + denominator : numerator + "";
        }
    }

    private int getMultiplicity(List<CraftingProcess> processes) {
        int res = 0;
        for (CraftingProcess process : processes) {
            res += process.getMultiplicity();
        }
        return res;
    }

    private boolean usingCraftingTable() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof CraftingScreenHandler;
    }

    private Node makeRootNode(Identifier identifier, int numNeeded) {
        Item item = Registry.ITEM.get(identifier);
        List<CraftingProcess> processes = getProcesses(identifier);
        return makeNode(null, new ItemStack(item, numNeeded), processes).setNeeded(numNeeded);
    }

    private Node makeNode(Node root, ItemStack stack, List<CraftingProcess> itemProcesses) {
        int newNeeded = 0;
        if (root != null && !(root instanceof ChoiceNode)) {
            newNeeded = new Fraction(root.getNeededToCraft(0) * stack.getCount(), root.getOutputCount()).ceil();
        }
        int multiplicity = getMultiplicity(itemProcesses);
        Node node = null;
        if (multiplicity > 1 && !(root instanceof ChoiceNode)) {
            node = new ChoiceNode(stack.getItem(), newNeeded, itemProcesses.size() > 0 ? itemProcesses : null);
        }
        else if (multiplicity == 0 || itemProcesses.get(0) instanceof RecipeCraftingProcess) {
            node = new RecipeNode(stack.getItem(), newNeeded, itemProcesses.size() > 0 ? itemProcesses : null);
        }
        else if (itemProcesses.get(0) instanceof StorageCraftingProcess) {
            node = new StorageNode(stack.getItem(), newNeeded, itemProcesses.size() > 0 ? itemProcesses : null);
        }
        return node;
    }

    private List<Node> getChildren(Node root, HashSet<Node> nodes) {
        List<Node> children = new ArrayList<>();
        List<CraftingProcess> processes = root.processes;
        if (processes == null)
            return children;
        if (root instanceof ChoiceNode) {
            for (CraftingProcess process : processes) {
                int m = process.getMultiplicity();
                ArrayList<CraftingProcess> processList = new ArrayList<>();
                processList.add(process);
                for (int i = 0; i < m; i++) {
                    Node child = makeNode(root, new ItemStack(root.target, root.needed), processList).setStackShift(i).setNeeded(root.needed);
                    children.add(child);
                }
            }
            return children;
        }
        else {
            HashMap<Item, ItemStack> ingredients = root.collectIngredients();
            for (ItemStack stack : ingredients.values()) {
                Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
                List<CraftingProcess> itemProcesses = getProcesses(itemIdentifier);
                Node child = makeNode(root, stack, itemProcesses);
                if (!nodes.contains(child)) {
                    children.add(child);
                }
            }
            return children;
        }
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

    public void queueCraft(Identifier itemId, int count, boolean craftAll) {
        synchronized(craftingQueue) {
            craftingQueue.add(new CraftingQueueEntry(itemId, count, craftAll));
        }
    }

    public void notifySlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
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

    // This could (should) be done in O(1) time by modifying verifyNode, but O(log n) time seems to work without lagging
    private int getMaxCraftable(Identifier itemId, CraftingState state) {
        int count = 1;
        while (makeRootNode(itemId, count).verifyPreliminary(state.clone()).success) {
            count *= 2;
        }
        count /= 2;
        int increment = count / 2;
        if (increment > 0)
            count--;
        while (increment > 0) {
            if (makeRootNode(itemId, count).verifyPreliminary(state.clone()).success) {
                count += increment;
            }
            else {
                count -= increment;
            }
            increment /= 2;
        }
        if (makeRootNode(itemId, count).verifyPreliminary(state.clone()).success) {
            while (makeRootNode(itemId, count).verifyPreliminary(state.clone()).success) {
                count++;
            }
            count--;
        }
        else {
            while (!makeRootNode(itemId, count).verifyPreliminary(state.clone()).success) {
                count--;
            }
        }
        return count;
    }

    private int getMaxCraftable(Node node, CraftingState state) {
        int originalNeeded = node.needed;
        int count = 1;
        while (node.setNeeded(count).verifyPreliminary(state.clone()).success) {
            count *= 2;
        }
        count /= 2;
        int increment = count / 2;
        if (increment > 0)
            count--;
        while (increment > 0) {
            if (node.setNeeded(count).verifyPreliminary(state.clone()).success) {
                count += increment;
            }
            else {
                count -= increment;
            }
            increment /= 2;
        }
        if (node.setNeeded(count).verifyPreliminary(state.clone()).success) {
            while (node.setNeeded(count).verifyPreliminary(state.clone()).success) {
                count++;
            }
            count--;
        }
        else {
            while (!node.setNeeded(count).verifyPreliminary(state.clone()).success) {
                count--;
            }
        }
        node.needed = originalNeeded;
        return count;
    }

    private void craft(Identifier itemId, int count, boolean craftAll) {
        isCurrentlyCrafting = true;
        new Thread(() -> {
            containerManager.updateContainers(worldQuery.getLocations(containerBlockTypes));
            Node root = makeRootNode(itemId, craftAll ? 1 : count);
            Item item = Registry.ITEM.get(itemId);
            int initialCount = inventoryAvailabilityMap.getOrDefault(item, 0);
            inventoryAvailabilityMap.put(item, 0);
            if (craftAll) {
                root = makeRootNode(itemId, getMaxCraftable(itemId, new CraftingState(inventoryAvailabilityMap, storageAvailabilityMap, new HashSet<>())));
            }
            CraftingState verificationState = root.verify();
            if (verificationState.success) {
                HashMap<Item, Integer> obtainFromStorage = new HashMap<>();
                for (StorageNode node : verificationState.storageNodes) {
                    obtainFromStorage.put(node.target, obtainFromStorage.getOrDefault(node.target, 0) + node.needed);
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
                        /*craftingQueue.get(0).count -= finalCount - initialCount;
                        if (craftingQueue.get(0).count <= 0)
                            craftingQueue.remove(0);*/
                    }
                    inventoryAvailabilityMap.put(item, inventoryAvailabilityMap.getOrDefault(item, 0) + initialCount);
                    isCurrentlyCrafting = false;
                    return;
                }
            }
            inventoryAvailabilityMap.put(item, inventoryAvailabilityMap.getOrDefault(item, 0) + initialCount);
            synchronized(craftingQueue) {
                craftingQueue.remove(0);
            }
            isCurrentlyCrafting = false;
        }).start();
    }

    @Override
    public void onEnable() {
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
                        totalInventoryAvailabilityMap = inventoryQuery.getAvailabilityMap();
                        inventoryAvailabilityMap = (HashMap<Item, Integer>) totalInventoryAvailabilityMap.clone();
                        storageAvailabilityMap = containerQuery.getAvailabilityMap();
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
