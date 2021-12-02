package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

import java.util.*;
import java.util.stream.Stream;

public class AutoCraftHack extends Hack implements UpdateListener {

    private HashMap<Identifier, List<Recipe<?>>> recipeMap = null;

    private List<CraftingQueueEntry> craftingQueue = new ArrayList<>();

    private boolean isCurrentlyCrafting = false;

    public AutoCraftHack() {
        super("AutoCraft");
        setCategory(Category.ITEMS);
    }

    private void initRecipeMap() {
        recipeMap = new HashMap<>();
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
                if (!recipeMap.containsKey(baseId))
                    recipeMap.put(baseId, new ArrayList<Recipe<?>>());
                recipeMap.get(baseId).add(recipe);
            }
            else {
                if (!recipeMap.containsKey(id))
                    recipeMap.put(id, new ArrayList<Recipe<?>>());
                recipeMap.get(id).add(recipe);
            }
        }
    }

    private abstract class StorageQuery {
        public abstract int getAvailable(Item item);
        public abstract HashMap<Item, Integer> getAvailabilityMap();
        public abstract void acquire(Item item, int count);
    }

    private class InventoryStorageQuery extends StorageQuery {
        public InventoryStorageQuery() { }
        @Override
        public int getAvailable(Item item) {
            int count = 0;
            List<ItemStack> items = MC.player.getInventory().main;
            for (ItemStack cur : items) {
                if (item.equals(cur.getItem())) {
                    count += cur.getCount();
                }
            }
            return count;
        }
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

    private class CraftingNode {
        private List<CraftingNode> children;
        private Item target;
        private Fraction count;
        private int needed;
        private List<Recipe<?>> recipes;
        private boolean isChoiceNode;
        private int stackShift;
        private boolean childOfChoiceNode;
        private CraftingNode parent;
        public CraftingNode(Item target, Fraction count, int needed, List<Recipe<?>> recipes) {
            isChoiceNode = false;
            childOfChoiceNode = false;
            stackShift = 0;
            parent = null;
            children = new ArrayList<>();
            this.target = target;
            this.count = count;
            this.recipes = recipes;
            this.needed = needed;
        }
        public CraftingNode setChoiceNode(boolean choiceNode) {
            isChoiceNode = choiceNode;
            return this;
        }
        public CraftingNode setStackShift(int stackShift) {
            this.stackShift = stackShift;
            return this;
        }
        public CraftingNode setChildOfChoiceNode(boolean childOfChoiceNode) {
            this.childOfChoiceNode = childOfChoiceNode;
            return this;
        }
        public void addChild(CraftingNode child) {
            child.parent = this;
            children.add(child);
        }
        @Override
        public int hashCode() {
            return target.hashCode();
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof CraftingNode) {
                CraftingNode o = (CraftingNode)other;
                return target.equals(o.target);
            }
            return false;
        }
        public void set(CraftingNode other) {
            this.children = other.children;
            this.target = other.target;
            this.count = other.count;
            this.recipes = other.recipes;
            this.needed = other.needed;
        }
        public HashMap<Item, ItemStack> collectIngredients(int index) {
            HashMap<Item, ItemStack> stackTypes = new HashMap<>();
            for (Ingredient ing : recipes.get(0).getIngredients()) {
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
        public int getNeededToCraft(Recipe<?> recipe) {
            if (needed < recipe.getOutput().getCount()) {
                return recipe.getOutput().getCount();
            }
            else {
                return (int)Math.ceil((double)needed / recipe.getOutput().getCount()) * recipe.getOutput().getCount();
            }
        }
        public boolean craft() {
            try {
                for (int i = 0; i < getNeededToCraft(recipes.get(0)) / recipes.get(0).getOutput().getCount(); i++) {
                    Thread.sleep(100);
                    if (!usingCraftingTable()) return false;
                    MC.interactionManager.clickRecipe(MC.player.currentScreenHandler.syncId, recipes.get(0), false);
                    Thread.sleep(100);
                    if (!usingCraftingTable()) return false;
                    MC.interactionManager.clickSlot(MC.player.currentScreenHandler.syncId, 0, 0, SlotActionType.QUICK_MOVE, MC.player);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    private int getMultiplicity(Recipe<?> recipe) {
        int res = 1;
        for (Ingredient ing : recipe.getIngredients()) {
            res = Math.max(res, ing.getMatchingStacks().length);
        }
        return res;
    }

    private int getMultiplicity(List<Recipe<?>> recipes) {
        int res = 0;
        for (Recipe<?> recipe : recipes) {
            res += getMultiplicity(recipe);
        }
        return res;
    }

    private void makeTree(CraftingNode root, HashSet<CraftingNode> nodes, boolean allowChoiceNode) {
        List<Recipe<?>> recipes = root.recipes;
        int multiplicity = getMultiplicity(recipes);
        if (multiplicity > 1 && !allowChoiceNode) {
            root.setChildOfChoiceNode(true);
        }
        if (multiplicity > 1 && !root.childOfChoiceNode) {
            root.setChoiceNode(true);
            for (Recipe<?> recipe : recipes) {
                int m = getMultiplicity(recipe);
                ArrayList<Recipe<?>> recipeList = new ArrayList<>();
                recipeList.add(recipe);
                for (int i = 0; i < m; i++) {
                    CraftingNode child = new CraftingNode(root.target, root.count, root.needed, recipeList).setStackShift(i).setChildOfChoiceNode(true);
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
            List<Recipe<?>> itemRecipes = recipeMap.getOrDefault(itemIdentifier, new ArrayList<Recipe<?>>());
            int newNeeded = new Fraction(root.getNeededToCraft(recipes.get(0)) * stack.getCount(), recipes.get(0).getOutput().getCount()).ceil();
            CraftingNode child = new CraftingNode(stack.getItem(), root.count.mult(new Fraction(stack.getCount())).div(new Fraction(recipes.get(0).getOutput().getCount())), newNeeded, itemRecipes.size() > 0 ? itemRecipes : null);
            if (!nodes.contains(child)) {
                root.addChild(child);
                if (itemRecipes.size() > 0) {
                    makeTree(child, nodes, true);
                }
            }
        }
        nodes.remove(root);
    }

    private CraftingNode generateCraftingTree(Identifier identifier, int numNeeded, boolean allowChoiceNode, int stackShift, HashSet<CraftingNode> nodes, List<Recipe<?>> recipes) {
        Item item = Registry.ITEM.get(identifier);
        if (recipes == null)
            recipes = recipeMap.getOrDefault(identifier, new ArrayList<>());
        if (recipes.size() == 0)
            return new CraftingNode(item, new Fraction(numNeeded), numNeeded, null);
        CraftingNode root = new CraftingNode(item, new Fraction(numNeeded), numNeeded, recipes).setStackShift(stackShift);
        if (nodes == null)
            nodes = new HashSet<>();
        makeTree(root, nodes, allowChoiceNode);
        return root;
    }

    private HashSet<CraftingNode> getAncestors(CraftingNode node) {
        HashSet<CraftingNode> res = new HashSet<>();
        if (node != null)
            node = node.parent;
        while (node != null) {
            res.add(node);
            node = node.parent;
        }
        return res;
    }

    private boolean verifyNode(CraftingNode node, HashMap<Item, Integer> availability) {
        int numAvailable = availability.getOrDefault(node.target, 0);
        if (numAvailable >= node.needed) {
            availability.put(node.target, availability.get(node.target) - node.needed);
            return true;
        }
        if (node.children.size() == 0)
            return false;
        if (numAvailable > 0) {
            availability.put(node.target, 0);
            node = generateCraftingTree(Registry.ITEM.getId(node.target), node.needed - numAvailable, !node.childOfChoiceNode, node.stackShift, getAncestors(node), node.recipes);
        }
        if (node.isChoiceNode) {
            for (CraftingNode child : node.children) {
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
            for (CraftingNode child : node.children) {
                if (!verifyNode(child, availability)) {
                    return false;
                }
            }
            availability.put(node.target, availability.getOrDefault(node.target, 0) + node.getNeededToCraft(node.recipes.get(0)) - node.needed);
            return true;
        }
    }

    private boolean usingCraftingTable() {
        return MC.player.currentScreenHandler != null && MC.player.currentScreenHandler instanceof CraftingScreenHandler;
    }

    private boolean craftNode(CraftingNode node, HashMap<Item, Integer> availability) {
        int numAvailable = availability.getOrDefault(node.target, 0);
        if (numAvailable >= node.needed) {
            availability.put(node.target, numAvailable - node.needed);
            return true;
        }
        if (numAvailable > 0 && !node.isChoiceNode) {
            availability.put(node.target, 0);
            node = generateCraftingTree(Registry.ITEM.getId(node.target), node.needed - numAvailable, !node.childOfChoiceNode, node.stackShift, getAncestors(node), node.recipes);
        }
        if (node.isChoiceNode) {
            for (CraftingNode child : node.children) {
                if (verifyNode(child, (HashMap<Item, Integer>)availability.clone())) {
                    craftNode(child, availability);
                    return true;
                }
            }
        }
        else {
            for (CraftingNode child : node.children) {
                craftNode(child, availability);
            }
            availability.put(node.target, availability.getOrDefault(node.target, 0) + node.getNeededToCraft(node.recipes.get(0)) - node.needed);
        }
        if (node.children.size() == 0)
            return true;
        if (!node.craft()) return false;
        return true;
    }

    private class CraftingQueueEntry {
        private Identifier itemId;
        private int count;
        public CraftingQueueEntry(Identifier itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
    }

    public void queueCraft(Identifier itemId, int count) {
        synchronized(craftingQueue) {
            craftingQueue.add(new CraftingQueueEntry(itemId, count));
        }
    }

    private void craft(Identifier itemId, int count) {
        isCurrentlyCrafting = true;
        new Thread(() -> {
            CraftingNode root = generateCraftingTree(itemId, count, true, 0, null, null);
            InventoryStorageQuery query = new InventoryStorageQuery();
            HashMap<Item, Integer> availability = query.getAvailabilityMap();
            Item item = Registry.ITEM.get(itemId);
            int initialCount = availability.getOrDefault(item, 0);
            availability.put(item, 0);
            if (verifyNode(root, (HashMap<Item, Integer>)availability.clone())) {
                if (!craftNode(root, availability)) {
                    int finalCount = query.getAvailable(item);
                    synchronized (craftingQueue) {
                        craftingQueue.get(0).count -= finalCount - initialCount;
                        if (craftingQueue.get(0).count <= 0)
                            craftingQueue.remove(0);
                    }
                    isCurrentlyCrafting = false;
                    return;
                }
            }
            synchronized(craftingQueue) {
                craftingQueue.remove(0);
            }
            isCurrentlyCrafting = false;
        }).start();
    }

    @Override
    public void onEnable() {
        if (recipeMap == null)
            initRecipeMap();
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onUpdate() {
        synchronized (craftingQueue) {
            if (!isCurrentlyCrafting && craftingQueue.size() > 0 && usingCraftingTable()) {
                CraftingQueueEntry entry = craftingQueue.get(0);
                craft(entry.itemId, entry.count);
            }
        }
    }
}
