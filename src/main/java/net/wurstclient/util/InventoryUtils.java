/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.function.Predicate;
import java.util.stream.IntStream;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;

public enum InventoryUtils
{
	;
	
	private static final MinecraftClient MC = WurstClient.MC;
	private static final IMinecraftClient IMC = WurstClient.IMC;
	
	public static boolean selectItem(Item item)
	{
		return selectItem(stack -> stack.getItem() == item, 36, false);
	}
	
	public static boolean selectItem(Item item, int maxInvSlot)
	{
		return selectItem(stack -> stack.getItem() == item, maxInvSlot, false);
	}
	
	public static boolean selectItem(Item item, int maxInvSlot,
		boolean takeFromOffhand)
	{
		return selectItem(stack -> stack.getItem() == item, maxInvSlot,
			takeFromOffhand);
	}
	
	public static boolean selectItem(Predicate<ItemStack> predicate)
	{
		return selectItem(predicate, 36, false);
	}
	
	public static boolean selectItem(Predicate<ItemStack> predicate,
		int maxInvSlot)
	{
		return selectItem(predicate, maxInvSlot, false);
	}
	
	/**
	 * Searches the player's inventory from slot 0 to {@code maxInvSlot-1} for
	 * the first item that matches the given predicate and moves it to
	 * {@code inventory.selectedSlot}.
	 *
	 * <p>
	 * <b>WARNING:</b> A return value of {@code true} does not necessarily mean
	 * that the item is now in the selected slot, only that it was found in the
	 * inventory. Always check that you are actually holding the item before
	 * attempting to use it.
	 *
	 * @param predicate
	 *            checks if an item is the one you want
	 * @param maxInvSlot
	 *            the maximum slot to search (exclusive), usually 9 for the
	 *            hotbar or 36 for the whole inventory
	 * @param takeFromOffhand
	 *            also search the offhand (slot 40), even if maxInvSlot is lower
	 * @return {@code true} if the item was found. This does not necessarily
	 *         mean that the item is now in the selected slot, it could still be
	 *         on its way there.
	 */
	public static boolean selectItem(Predicate<ItemStack> predicate,
		int maxInvSlot, boolean takeFromOffhand)
	{
		PlayerInventory inventory = MC.player.inventory;
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		// create a stream of all slots that we want to search
		IntStream stream = IntStream.range(0, maxInvSlot);
		if(takeFromOffhand)
			stream = IntStream.concat(stream, IntStream.of(40));
		
		// find the slot of the item we want
		int slot = stream.filter(i -> predicate.test(inventory.getStack(i)))
			.findFirst().orElse(-1);
		
		// if no item was found, return false
		if(slot == -1)
			return false;
		
		// if the item is already in the hotbar, just select it
		if(slot < 9)
			inventory.selectedSlot = slot;
		// if there is an empty slot in the hotbar, shift-click the item there
		// it will be selected in the next tick
		else if(inventory.getEmptySlot() > -1 && inventory.getEmptySlot() < 9)
			im.windowClick_QUICK_MOVE(slot);
		// otherwise, swap with the currently selected item
		else
			im.windowClick_SWAP(slot, inventory.selectedSlot);
		
		return false;
	}
}
