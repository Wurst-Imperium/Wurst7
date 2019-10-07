package net.wurstclient.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.AirBlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class WItem
{
	public static boolean isNullOrEmpty(Item item)
	{
		return item == null || item instanceof AirBlockItem;
	}
	
	public static boolean isNullOrEmpty(ItemStack stack)
	{
		return stack == null || stack.isEmpty();
	}
	
	public static ItemStack getItemStack(ItemEntity entityItem)
	{
		return entityItem.getStack();
	}
	
	public static int getStackSize(ItemStack stack)
	{
		return stack.getCount();
	}
	
	public static float getDestroySpeed(ItemStack stack, BlockState state)
	{
		return isNullOrEmpty(stack) ? 1 : stack.getMiningSpeed(state);
	}
}