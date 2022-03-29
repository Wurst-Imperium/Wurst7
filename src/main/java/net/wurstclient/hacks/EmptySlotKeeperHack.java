package net.wurstclient.hacks;

import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;

import static net.wurstclient.util.InventoryUtils.getAdjustedInventorySlot;

@SearchTags({"inventory", "empty", "slot", "pick"})
public class EmptySlotKeeperHack extends Hack implements UpdateListener
{
    public EmptySlotKeeperHack()
    {
        super("EmptySlotKeeper");
    }

    @Override
    public void onEnable()
    {
        disabledOnStartup = true;
        showRemoveItemMessage = true;
        EVENTS.add(UpdateListener.class, this);
    }

    @Override
    public void onDisable()
    {
        EVENTS.remove(UpdateListener.class, this);
    }

    private int lastEmptySlot = -1;
    private boolean disabledOnStartup;
    private boolean showRemoveItemMessage;

    private void keepOneSlotEmpty()
    {
        if(isInventoryFull())
        {
            //throw last picked up item away
            IMC.getInteractionManager().windowClick_THROW(getAdjustedInventorySlot(lastEmptySlot));
        }
    }

    private boolean isInventoryFull(){
        int slot = MC.player.getInventory().getEmptySlot();

        //no empty slot was found
        if(slot == -1)
            return true;

        lastEmptySlot = slot;
        return false;
    }

    @Override
    public void onUpdate()
    {
        if(disabledOnStartup)
        {
            checkInventoryBeforeStart();
        }
        else
        {
            keepOneSlotEmpty();
        }
    }

    private void checkInventoryBeforeStart()
    {
        //Forces the user to remove one item of choice if the inventory is full after enabling
        if(isInventoryFull())
        {
            if(showRemoveItemMessage)
            {
                ChatUtils.warning("Please remove one item from your inventory before the EmptySlotKeeper can start.");
                showRemoveItemMessage = false;
            }
        }
        else
        {
            disabledOnStartup = false;
        }
    }
}