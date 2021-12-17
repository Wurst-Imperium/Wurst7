package net.wurstclient.hacks;

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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.commands.GoToCmd;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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

    private HashMap<Item, Integer> availabilityMap = new HashMap<>();
    private HashMap<Item, Integer> totalAvailabilityMap = new HashMap<>();

    private Pathfinder pathFinder = new WurstPathfinder();

    private BlockPos latestBlockPos = BlockPos.ORIGIN;

    private boolean doneCrafting = true;

    public AutoCraftHack() {
        super("AutoCraft");
        setCategory(Category.ITEMS);
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

    private class SlotUpdateInfo {
        public int slot;
        public ItemStack itemStack;
        public SlotUpdateInfo(int slot, ItemStack itemStack) {
            this.slot = slot;
            this.itemStack = itemStack;
        }
    }

    private abstract class StorageQuery {
        public abstract HashMap<Item, Integer> getAvailabilityMap();
        public abstract void acquire(Item item, int count);
    }

    private class InventoryStorageQuery extends StorageQuery {
        public InventoryStorageQuery() { }
        @Override
        public HashMap<Item, Integer> getAvailabilityMap() {
            HashMap<Item, Integer> res = new HashMap<>();
            List<ItemStack> items = MC.player.getInventory().main;
            for (ItemStack cur : items) {
                res.put(cur.getItem(), res.getOrDefault(cur.getItem(), 0) + cur.getCount());
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

    private class ContainerStorageQuery extends StorageQuery {
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
        private int takeItem(Item item, int count) {
            ScreenHandler handler = MC.player.currentScreenHandler;
            for (int i = 0; i < handler.slots.size() - 36; i++) {
                if (count <= 0)
                    break;
                if (handler.getSlot(i).hasStack() && handler.getSlot(i).getStack().getItem().equals(item)) {
                    count -= handler.getSlot(i).getStack().getCount();
                    totalAvailabilityMap.put(item, totalAvailabilityMap.getOrDefault(item, 0) + handler.getSlot(i).getStack().getCount());
                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, MC.player);
                }
            }
            return count;
        }
        private void closeScreen() {
            try {
                MC.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(MC.player.currentScreenHandler.syncId));
                MC.getNetworkHandler().onCloseScreen(new CloseScreenS2CPacket(MC.player.currentScreenHandler.syncId));
            }
            catch (OffThreadException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void acquire(Item item, int count) {
            for (BlockPos pos : containers.keySet()) {
                if (count <= 0)
                    break;
                if (containers.get(pos).getOrDefault(item, 0) > 0) {
                    pathFinder.path(pos.up());
                    IMC.getInteractionManager().rightClickBlock(pos, Direction.NORTH, Vec3d.ZERO);
                    awaitContainerOpen();
                    count = takeItem(item, count);
                    closeScreen();
                }
            }
        }
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
        public boolean craft() {
            HashMap<Item, Integer> availability = availabilityMap;
            HashSet<Node> visited = new HashSet<>();
            return craftPreliminary(availability, visited);
        }
        public boolean verify() {
            HashMap<Item, Integer> availability = (HashMap<Item, Integer>)availabilityMap.clone();
            HashSet<Node> visited = new HashSet<>();
            return verifyPreliminary(availability, visited);
        }
        private boolean craftPreliminary(HashMap<Item, Integer> availability, HashSet<Node> visited) {
            int numAvailable = availability.getOrDefault(target, 0);
            if (numAvailable >= needed) {
                availability.put(target, numAvailable - needed);
                return true;
            }
            if (numAvailable > 0) {
                availability.put(target, 0);
                needed -= numAvailable;
            }
            List<Node> children = getChildren(this, visited);
            if (craftInternal(availability, visited, children))
                return true;
            if (children.size() == 0)
                return true;
            return execute();
        }
        private boolean verifyPreliminary(HashMap<Item, Integer> availability, HashSet<Node> visited) {
            if (needed <= 0)
                return true;
            int numAvailable = availability.getOrDefault(target, 0);
            int neededOffset = 0;
            if (numAvailable >= needed) {
                availability.put(target, availability.get(target) - needed);
                return true;
            }
            List<Node> children = getChildren(this, visited);
            if (children.size() == 0)
                return false;
            if (numAvailable > 0) {
                availability.put(target, 0);
                needed -= numAvailable;
                neededOffset += numAvailable;
                children = getChildren(this, visited);
            }
            boolean res = verifyInternal(availability, visited, children);
            needed += neededOffset;
            return res;
        }
        protected abstract boolean craftInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children);
        protected abstract boolean verifyInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children);
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
        protected boolean craftInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children) {
            visited.add(this);
            for (Node child : children) {
                child.craftPreliminary(availability, visited);
            }
            visited.remove(this);
            availability.put(target, availability.getOrDefault(target, 0) + getNeededToCraft(0) - needed);
            return false;
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
        protected boolean verifyInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children) {
            visited.add(this);
            for (Node child : children) {
                if (!child.verifyPreliminary(availability, visited)) {
                    visited.remove(this);
                    return false;
                }
            }
            visited.remove(this);
            availability.put(target, availability.getOrDefault(target, 0) + getNeededToCraft(0) - needed);
            return true;
        }
        @Override
        public int getOutputCount() {
            return ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
        }
        private void awaitSlotUpdate(Item item, int amount, int slot, boolean onlyConsiderItem) {
            slotUpdateLock.lock();
            try {
                while (latestSlotUpdate == null || !latestSlotUpdate.itemStack.getItem().equals(item) || (!onlyConsiderItem && (latestSlotUpdate.itemStack.getCount() != amount || latestSlotUpdate.slot != slot))) {
                    boolean gotSignal = slotUpdateCondition.await(1000, TimeUnit.MILLISECONDS);
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
        private int calculateCraftingOutput() {
            List<Ingredient> ingredients = ((RecipeCraftingProcess)processes.get(0)).recipe.getIngredients();
            HashMap<Item, ItemStack> collected = collectIngredients();
            int output = Integer.MAX_VALUE;
            for (Ingredient ing : ingredients) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack itemStack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                int outputFactor = Math.min(totalAvailabilityMap.getOrDefault(itemStack.getItem(), 0) / collected.get(itemStack.getItem()).getCount(), itemStack.getItem().getMaxCount()) * ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
                output = Math.min(output, outputFactor);
            }
            return output;
        }
        private void adjustTotalAvailability(Recipe<?> recipe, int craftingOutput) {
            Item outputItem = recipe.getOutput().getItem();
            totalAvailabilityMap.put(outputItem, totalAvailabilityMap.getOrDefault(outputItem, 0) + craftingOutput);
            for (Ingredient ing : recipe.getIngredients()) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack stack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                totalAvailabilityMap.put(stack.getItem(), totalAvailabilityMap.getOrDefault(stack.getItem(), 0) - (stack.getCount() * craftingOutput) / recipe.getOutput().getCount());
            }
        }
        @Override
        public boolean execute() {
            int neededToCraft = getNeededToCraft(0);
            int craftingOutput = 0;
            while ((craftingOutput = calculateCraftingOutput()) <= neededToCraft && craftingOutput > 0) {
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickRecipe(MC.player.currentScreenHandler.syncId, ((RecipeCraftingProcess)processes.get(0)).recipe, true);
                awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false);
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true);
                adjustTotalAvailability(((RecipeCraftingProcess)processes.get(0)).recipe, craftingOutput);
                neededToCraft -= craftingOutput;
            }
            for (int i = 0; i < neededToCraft / ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(); i++) {
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickRecipe(MC.player.currentScreenHandler.syncId, ((RecipeCraftingProcess)processes.get(0)).recipe, false);
                awaitSlotUpdate(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getItem(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount(), 0, false);
                if (!usingCraftingTable()) return false;
                MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                awaitSlotUpdate(Registry.ITEM.get(new Identifier("minecraft", "air")), 0, 0, true);
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
        protected boolean craftInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children) {
            for (Node child : children) {
                int original = child.needed;
                child.needed = needed;
                boolean verification = child.verifyPreliminary((HashMap<Item, Integer>)availability.clone(), visited);
                if (!verification) {
                    int maxCraftable = getMaxCraftable(child, (HashMap<Item, Integer>)availability.clone());
                    if (maxCraftable > 0) {
                        child.needed = maxCraftable;
                        child.craftPreliminary(availability, visited);
                        needed -= maxCraftable;
                    }
                }
                else {
                    child.craftPreliminary(availability, visited);
                    child.needed = original;
                    return true;
                }
                child.needed = original;
            }
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        protected boolean verifyInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children) {
            for (Node child : children) {
                HashMap<Item, Integer> newAvailability = (HashMap<Item, Integer>)availability.clone();
                boolean verification = child.verifyPreliminary(newAvailability, visited);
                if (!verification) {
                    int maxCraftable = getMaxCraftable(child, (HashMap<Item, Integer>)availability.clone());
                    int originalNeeded = child.needed;
                    child.needed = maxCraftable;
                    child.verifyPreliminary(availability, visited);
                    child.needed = originalNeeded;
                    needed -= maxCraftable;
                }
                else {
                    availability.clear();
                    availability.putAll(newAvailability);
                    return true;
                }
                if (needed <= 0)
                    return true;
            }
            return false;
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
        protected boolean craftInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children) {
            return false;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients() {
            return new HashMap<>();
        }
        @Override
        protected boolean verifyInternal(HashMap<Item, Integer> availability, HashSet<Node> visited, List<Node> children) {
            return containerQuery.getAvailabilityMap().getOrDefault(target, 0) >= needed;
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
        List<CraftingProcess> processes = processMap.getOrDefault(identifier, new ArrayList<>());
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
                List<CraftingProcess> itemProcesses = processMap.getOrDefault(itemIdentifier, new ArrayList<>());
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
        /*new Thread(() -> {
            containerQuery.acquire(Registry.ITEM.get(new Identifier("minecraft", "iron_ingot")), 1);
        }).start();*/
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

    public void storageContainerAccessed(int syncId) { }

    public void storageContainerContent(int syncId, List<ItemStack> content) {
        HashMap<Item, Integer> contentMap = new HashMap<>();
        for (int i = 0; i < content.size() - 36; i++) {
            ItemStack stack = content.get(i);
            if (stack.getCount() > 0)
                contentMap.put(stack.getItem(), contentMap.getOrDefault(stack.getItem(), 0) + stack.getCount());
        }
        containerQuery.updateContainer(contentMap, latestBlockPos);
        containerOpenLock.lock();
        try {
            containerOpenCondition.signalAll();
        }
        finally {
            containerOpenLock.unlock();
        }
    }

    public void blockPositionClicked(BlockPos pos) {
        latestBlockPos = pos;
    }

    // This could (should) be done in O(1) time by modifying verifyNode, but O(log n) time seems to work without lagging
    private int getMaxCraftable(Identifier itemId, HashMap<Item, Integer> availability) {
        int count = 1;
        while (makeRootNode(itemId, count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
            count *= 2;
        }
        count /= 2;
        int increment = count / 2;
        if (increment > 0)
            count--;
        while (increment > 0) {
            if (makeRootNode(itemId, count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
                count += increment;
            }
            else {
                count -= increment;
            }
            increment /= 2;
        }
        if (makeRootNode(itemId, count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
            while (makeRootNode(itemId, count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
                count++;
            }
            count--;
        }
        else {
            while (!makeRootNode(itemId, count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
                count--;
            }
        }
        return count;
    }

    private int getMaxCraftable(Node node, HashMap<Item, Integer> availability) {
        int originalNeeded = node.needed;
        int count = 1;
        while (node.setNeeded(count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
            count *= 2;
        }
        count /= 2;
        int increment = count / 2;
        if (increment > 0)
            count--;
        while (increment > 0) {
            if (node.setNeeded(count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
                count += increment;
            }
            else {
                count -= increment;
            }
            increment /= 2;
        }
        if (node.setNeeded(count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
            while (node.setNeeded(count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
                count++;
            }
            count--;
        }
        else {
            while (!node.setNeeded(count).verifyPreliminary((HashMap<Item, Integer>) availability.clone(), new HashSet<>())) {
                count--;
            }
        }
        node.needed = originalNeeded;
        return count;
    }

    private void craft(Identifier itemId, int count, boolean craftAll) {
        isCurrentlyCrafting = true;
        new Thread(() -> {
            Node root = makeRootNode(itemId, craftAll ? 1 : count);
            Item item = Registry.ITEM.get(itemId);
            int initialCount = availabilityMap.getOrDefault(item, 0);
            availabilityMap.put(item, 0);
            if (craftAll) {
                root = makeRootNode(itemId, getMaxCraftable(itemId, availabilityMap));
            }
            if (root.verify()) {
                if (!root.craft()) {
                    int finalCount = totalAvailabilityMap.getOrDefault(item, 0);
                    synchronized (craftingQueue) {
                        craftingQueue.get(0).count -= finalCount - initialCount;
                        if (craftingQueue.get(0).count <= 0)
                            craftingQueue.remove(0);
                    }
                    availabilityMap.put(item, availabilityMap.getOrDefault(item, 0) + initialCount);
                    isCurrentlyCrafting = false;
                    return;
                }
            }
            availabilityMap.put(item, availabilityMap.getOrDefault(item, 0) + initialCount);
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
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onUpdate() {
        synchronized (craftingQueue) {
            if (!isCurrentlyCrafting) {
                if (craftingQueue.size() > 0 && usingCraftingTable()) {
                    CraftingQueueEntry entry = craftingQueue.get(0);
                    if (doneCrafting) {
                        totalAvailabilityMap = inventoryQuery.getAvailabilityMap();
                        availabilityMap = (HashMap<Item, Integer>)totalAvailabilityMap.clone();
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
