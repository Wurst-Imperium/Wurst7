package net.wurstclient.util;

public enum InventoryUtils
{
    ;

    public static int getAdjustedInventorySlot(int slot)
    {
        return isHotbarSlot(slot) ? slot + 36 : slot;
    }

    public static boolean isHotbarSlot(int slot)
    {
        return slot >= 0 && slot <= 8;
    }
}