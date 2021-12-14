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
        protected List<Node> children;
        protected Item target;
        protected Fraction count;
        protected int needed;
        protected boolean isChoiceNode;
        protected boolean childOfChoiceNode;
        protected Node parent;
        protected List<CraftingProcess> processes;
        public Node(Item target, Fraction count, int needed, List<CraftingProcess> processes) {
            isChoiceNode = false;
            childOfChoiceNode = false;
            parent = null;
            children = new ArrayList<>();
            this.processes = processes;
            this.target = target;
            this.count = count;
            this.needed = needed;
        }
        public Node setChoiceNode(boolean choiceNode) {
            isChoiceNode = choiceNode;
            return this;
        }
        public Node setChildOfChoiceNode(boolean childOfChoiceNode) {
            this.childOfChoiceNode = childOfChoiceNode;
            return this;
        }
        public void addChild(Node child) {
            child.parent = this;
            children.add(child);
        }
        public abstract HashMap<Item, ItemStack> collectIngredients(int index);
        public abstract boolean execute();
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
        private int stackShift;
        public RecipeNode(Item target, Fraction count, int needed, List<CraftingProcess> processes) {
            super(target, count, needed, processes);
            stackShift = 0;
        }
        public RecipeNode setStackShift(int stackShift) {
            this.stackShift = stackShift;
            return this;
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients(int index) {
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
        public int getNeededToCraft(RecipeCraftingProcess process) {
            if (needed < process.recipe.getOutput().getCount()) {
                return process.recipe.getOutput().getCount();
            }
            else {
                return (int)Math.ceil((double)needed / process.recipe.getOutput().getCount()) * process.recipe.getOutput().getCount();
            }
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
            HashMap<Item, ItemStack> collected = collectIngredients(0);
            int output = Integer.MAX_VALUE;
            for (Ingredient ing : ingredients) {
                if (ing.getMatchingStacks().length == 0)
                    continue;
                ItemStack itemStack = ing.getMatchingStacks()[stackShift % ing.getMatchingStacks().length];
                int outputFactor = Math.min(totalAvailabilityMap.getOrDefault(itemStack.getItem(), 0) / collected.get(itemStack.getItem()).getCount(), 64) * ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount();
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
            int neededToCraft = getNeededToCraft(((RecipeCraftingProcess)processes.get(0)));
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

    private class StorageNode extends Node {
        public StorageNode(Item target, Fraction count, int needed, List<CraftingProcess> processes) {
            super(target, count, needed, processes);
        }
        @Override
        public HashMap<Item, ItemStack> collectIngredients(int index) {
            return new HashMap<>();
        }
        @Override
        public boolean execute() {
            containerQuery.acquire(target, needed);
            return true;
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

    private void makeTree(Node root, HashSet<Node> nodes, boolean allowChoiceNode) {
        List<CraftingProcess> processes = root.processes;
        int multiplicity = getMultiplicity(processes);
        if (multiplicity > 1 && !allowChoiceNode) {
            root.setChildOfChoiceNode(true);
        }
        if (multiplicity > 1 && !root.childOfChoiceNode) {
            root.setChoiceNode(true);
            for (CraftingProcess process : processes) {
                int m = process.getMultiplicity();
                ArrayList<CraftingProcess> processList = new ArrayList<>();
                processList.add(process);
                for (int i = 0; i < m; i++) {
                    Node child = new RecipeNode(root.target, root.count, root.needed, processList).setStackShift(i).setChildOfChoiceNode(true);
                    root.addChild(child);
                    makeTree(child, nodes, true);
                }
            }
            return;
        }
        nodes.add(root);
        HashMap<Item, ItemStack> ingredients = root.collectIngredients(0);
        for (ItemStack stack : ingredients.values()) {
            Identifier itemIdentifier = Registry.ITEM.getId(stack.getItem());
            List<CraftingProcess> itemProcesses = processMap.getOrDefault(itemIdentifier, new ArrayList<CraftingProcess>());
            int newNeeded = new Fraction(((RecipeNode)root).getNeededToCraft((RecipeCraftingProcess)processes.get(0)) * stack.getCount(), ((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount()).ceil();
            Node child = new RecipeNode(stack.getItem(), root.count.mult(new Fraction(stack.getCount())).div(new Fraction(((RecipeCraftingProcess)processes.get(0)).recipe.getOutput().getCount())), newNeeded, itemProcesses.size() > 0 ? itemProcesses : null);
            if (!nodes.contains(child)) {
                root.addChild(child);
                if (itemProcesses.size() > 0) {
                    makeTree(child, nodes, true);
                }
            }
        }
        nodes.remove(root);
    }

    private Node generateCraftingTree(Identifier identifier, int numNeeded, boolean allowChoiceNode, int stackShift, HashSet<Node> nodes, List<CraftingProcess> processes) {
        Item item = Registry.ITEM.get(identifier);
        if (processes == null)
            processes = processMap.getOrDefault(identifier, new ArrayList<>());
        if (processes.size() == 0)
            return new RecipeNode(item, new Fraction(numNeeded), numNeeded, null);
        Node root = new RecipeNode(item, new Fraction(numNeeded), numNeeded, processes).setStackShift(stackShift);
        if (nodes == null)
            nodes = new HashSet<>();
        makeTree(root, nodes, allowChoiceNode);
        return root;
    }

    private HashSet<Node> getAncestors(Node node) {
        HashSet<Node> res = new HashSet<>();
        if (node != null)
            node = node.parent;
        while (node != null) {
            res.add(node);
            node = node.parent;
        }
        return res;
    }

    private boolean verifyNode(Node node, HashMap<Item, Integer> availability) {
        int numAvailable = availability.getOrDefault(node.target, 0);
        if (numAvailable >= node.needed) {
            availability.put(node.target, availability.get(node.target) - node.needed);
            return true;
        }
        if (node.children.size() == 0)
            return false;
        if (numAvailable > 0) {
            availability.put(node.target, 0);
            node = generateCraftingTree(Registry.ITEM.getId(node.target), node.needed - numAvailable, !node.childOfChoiceNode, ((RecipeNode)node).stackShift, getAncestors(node), node.processes);
        }
        if (node.isChoiceNode) {
            for (Node child : node.children) {
                HashMap<Item, Integer> newAvailability = (HashMap<Item, Integer>)availability.clone();
                if (verifyNode(child, newAvailability)) {
                    availability.clear();
                    availability.putAll(newAvailability);
                    return true;
                }
            }
            return false;
        }
        else {
            for (Node child : node.children) {
                if (!verifyNode(child, availability)) {
                    return false;
                }
            }
            availability.put(node.target, availability.getOrDefault(node.target, 0) + ((RecipeNode)node).getNeededToCraft((RecipeCraftingProcess)node.processes.get(0)) - node.needed);
            return true;
        }
    }

    private boolean usingCraftingTable() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof CraftingScreenHandler;
    }

    private boolean craftNode(Node node, HashMap<Item, Integer> availability) {
        int numAvailable = availability.getOrDefault(node.target, 0);
        if (numAvailable >= node.needed) {
            availability.put(node.target, numAvailable - node.needed);
            return true;
        }
        if (numAvailable > 0 && !node.isChoiceNode) {
            availability.put(node.target, 0);
            node = generateCraftingTree(Registry.ITEM.getId(node.target), node.needed - numAvailable, !node.childOfChoiceNode, ((RecipeNode)node).stackShift, getAncestors(node), node.processes);
        }
        if (node.isChoiceNode) {
            for (Node child : node.children) {
                if (verifyNode(child, (HashMap<Item, Integer>)availability.clone())) {
                    craftNode(child, availability);
                    return true;
                }
            }
        }
        else {
            for (Node child : node.children) {
                craftNode(child, availability);
            }
            availability.put(node.target, availability.getOrDefault(node.target, 0) + ((RecipeNode)node).getNeededToCraft((RecipeCraftingProcess)node.processes.get(0)) - node.needed);
        }
        if (node.children.size() == 0)
            return true;
        if (!node.execute()) return false;
        return true;
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
        while (verifyNode(generateCraftingTree(itemId, count, true, 0, null, null), (HashMap<Item, Integer>) availability.clone())) {
            count *= 2;
        }
        count /= 2;
        int increment = count / 2;
        if (increment > 0)
            count--;
        while (increment > 0) {
            if (verifyNode(generateCraftingTree(itemId, count, true, 0, null, null), (HashMap<Item, Integer>)availability.clone())) {
                count += increment;
            }
            else {
                count -= increment;
            }
            increment /= 2;
        }
        if (verifyNode(generateCraftingTree(itemId, count, true, 0, null, null), (HashMap<Item, Integer>)availability.clone())) {
            while (verifyNode(generateCraftingTree(itemId, count, true, 0, null, null), (HashMap<Item, Integer>) availability.clone())) {
                count++;
            }
            count--;
        }
        else {
            while (!verifyNode(generateCraftingTree(itemId, count, true, 0, null, null), (HashMap<Item, Integer>) availability.clone())) {
                count--;
            }
        }
        return count;
    }

    private void craft(Identifier itemId, int count, boolean craftAll) {
        isCurrentlyCrafting = true;
        new Thread(() -> {
            Node root = generateCraftingTree(itemId, craftAll ? 1 : count, true, 0, null, null);
            Item item = Registry.ITEM.get(itemId);
            int initialCount = availabilityMap.getOrDefault(item, 0);
            availabilityMap.put(item, 0);
            if (craftAll) {
                root = generateCraftingTree(itemId, getMaxCraftable(itemId, availabilityMap), true, 0, null, null);
            }
            if (verifyNode(root, (HashMap<Item, Integer>)availabilityMap.clone())) {
                if (!craftNode(root, availabilityMap)) {
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
