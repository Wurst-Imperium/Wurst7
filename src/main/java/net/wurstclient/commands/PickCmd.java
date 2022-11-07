package net.wurstclient.commands;

import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.MathUtils;

import java.util.*;

import static net.wurstclient.util.InventoryUtils.isHotbarSlot;

public class PickCmd extends Command
{
    public PickCmd()
    {
        super("pick",
                "Picks the given item from your inventory\n" +
                        "to the hotbar slot of your choice.\n" +
                        "Requires 1 empty slot in your inventory and\n" +
                        "a filled hotbar.\n" +
                        "Use EmptySlotKeeper-Hack to automatically keep\n" +
                        "1 slot empty.",
                ".pick <item> <hotbar-slot>");
    }

    @Override
    public void call(String[] args) throws CmdException
    {
        if(args.length != 2)
            throw new CmdSyntaxError("Expected 2 arguments");

        Integer slot;
        String item = args[0];

        if(!MathUtils.isInteger(args[1]))
            throw new CmdSyntaxError("Slot has to be a number .");

        slot = Integer.parseInt(args[1]);

        if (!isHotbarSlot(slot))
            throw new CmdSyntaxError("Slot must be a number between 1 and 9.");

        slot--; //fix index to start at 0
        equipItem(item, slot);
    }

    private void equipItem(String item, Integer slot)
    {
        if(equipFromHotbar(item, slot))
        {
            return;
        }
        else
        {
            equipFromInventory(item, slot);
        }
    }

    private void equipFromInventory(String item, int slot)
    {
        // search potion in inventory
        int itemInInventory = findItem(9, 36,item );

        if(itemInInventory == -1)
            return;

        swapFromInventoryToHotbar(itemInInventory, slot);
    }

    private boolean equipFromHotbar(String item, int slot)
    {
        int itemInHotbar = findItem(0, 9, item);

        // check if any item was found
        if (itemInHotbar == -1)
            return false;

        //Currently the specified slot in which the item should be placed is ignored for hotbar items.
        //To fix this, you need to swap/click items in the hotbar. Unfortunately i wasn't able to code it.
        MC.player.inventory.selectedSlot = itemInHotbar;

        return true;
    }

    private void swapFromInventoryToHotbar(int from, int to)
    {
        if(from == -1)
            return;

        MC.player.inventory.selectedSlot = to;

        //1. move the item from the hotbar which is blocking the specified slot to your inventory (requires 1 empty space)
        IMC.getInteractionManager()
                .windowClick_QUICK_MOVE(to + 36);

        //2. move the target item from your inventory to the empty hotbar slot (all other hotbar slots must be filled)
        if (to != -1)
            IMC.getInteractionManager()
                    .windowClick_QUICK_MOVE(from);
    }

    private int findItem(int startSlot, int endSlot, String itemName)
    {
        for(int i = startSlot; i < endSlot; i++)
        {
            Item currentItem = MC.player.inventory.getInvStack(i).getItem();
            String currentItemName = Registry.ITEM.getId(currentItem).toString();
            if(Objects.equals(itemName, currentItemName)) {
                return i;
            }
        }
        return -1;
    }
}