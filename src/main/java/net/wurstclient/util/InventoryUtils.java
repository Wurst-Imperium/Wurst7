/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.function.Predicate;
import java.util.stream.IntStream;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;

public enum InventoryUtils
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	private static final IMinecraftClient IMC = WurstClient.IMC;
	
	public static int indexOf(Item item)
	{
		return indexOf(stack -> stack.is(item), 36, false);
	}
	
	public static int indexOf(Item item, int maxInvSlot)
	{
		return indexOf(stack -> stack.is(item), maxInvSlot, false);
	}
	
	public static int indexOf(Item item, int maxInvSlot, boolean includeOffhand)
	{
		return indexOf(stack -> stack.is(item), maxInvSlot, includeOffhand);
	}
	
	public static int indexOf(Predicate<ItemStack> predicate)
	{
		return indexOf(predicate, 36, false);
	}
	
	public static int indexOf(Predicate<ItemStack> predicate, int maxInvSlot)
	{
		return indexOf(predicate, maxInvSlot, false);
	}
	
	/**
	 * Searches the player's inventory from slot 0 to {@code maxInvSlot-1} for
	 * the first item that matches the given predicate and returns its slot, or
	 * -1 if no such item was found.
	 *
	 * @param predicate
	 *            checks if an item is the one you want
	 * @param maxInvSlot
	 *            the maximum slot to search (exclusive), usually 9 for the
	 *            hotbar or 36 for the whole inventory
	 * @param includeOffhand
	 *            also search the offhand (slot 40), even if maxInvSlot is lower
	 * @return
	 *         the slot of the item, or -1 if no such item was found
	 */
	public static int indexOf(Predicate<ItemStack> predicate, int maxInvSlot,
		boolean includeOffhand)
	{
		return getMatchingSlots(predicate, maxInvSlot, includeOffhand)
			.findFirst().orElse(-1);
	}
	
	public static int count(Item item)
	{
		return count(stack -> stack.is(item), 36, false);
	}
	
	public static int count(Item item, int maxInvSlot)
	{
		return count(stack -> stack.is(item), maxInvSlot, false);
	}
	
	public static int count(Item item, int maxInvSlot, boolean includeOffhand)
	{
		return count(stack -> stack.is(item), maxInvSlot, includeOffhand);
	}
	
	public static int count(Predicate<ItemStack> predicate)
	{
		return count(predicate, 36, false);
	}
	
	public static int count(Predicate<ItemStack> predicate, int maxInvSlot)
	{
		return count(predicate, maxInvSlot, false);
	}
	
	/**
	 * Counts the number of items in the player's inventory that match the given
	 * predicate, searching from slot 0 to {@code maxInvSlot-1}.
	 *
	 * <p>
	 * Note: Attempting to count empty slots will always return 0.
	 *
	 * @param predicate
	 *            checks if an item should be counted
	 * @param maxInvSlot
	 *            the maximum slot to search (exclusive), usually 9 for the
	 *            hotbar or 36 for the whole inventory
	 * @param includeOffhand
	 *            also search the offhand (slot 40), even if maxInvSlot is lower
	 * @return
	 *         the number of matching items in the player's inventory
	 */
	public static int count(Predicate<ItemStack> predicate, int maxInvSlot,
		boolean includeOffhand)
	{
		Inventory inventory = MC.player.getInventory();
		
		return getMatchingSlots(predicate, maxInvSlot, includeOffhand)
			.map(slot -> inventory.getItem(slot).getCount()).sum();
	}
	
	private static IntStream getMatchingSlots(Predicate<ItemStack> predicate,
		int maxInvSlot, boolean includeOffhand)
	{
		Inventory inventory = MC.player.getInventory();
		
		// create a stream of all slots that we want to search
		IntStream stream = IntStream.range(0, maxInvSlot);
		if(includeOffhand)
			stream = IntStream.concat(stream, IntStream.of(40));
		
		// filter out the slots we don't want
		return stream.filter(i -> predicate.test(inventory.getItem(i)));
	}
	
	public static boolean selectItem(Item item)
	{
		return selectItem(stack -> stack.is(item), 36, false);
	}
	
	public static boolean selectItem(Item item, int maxInvSlot)
	{
		return selectItem(stack -> stack.is(item), maxInvSlot, false);
	}
	
	public static boolean selectItem(Item item, int maxInvSlot,
		boolean takeFromOffhand)
	{
		return selectItem(stack -> stack.is(item), maxInvSlot, takeFromOffhand);
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
		return selectItem(indexOf(predicate, maxInvSlot, takeFromOffhand));
	}
	
	/**
	 * Moves the item in the given slot to {@code inventory.selectedSlot}. If
	 * the given slot is negative, this method will do nothing and return
	 * {@code false}.
	 *
	 * @param slot
	 *            the slot of the item to select
	 * @return {@code true} if the item was moved. This does not necessarily
	 *         mean that the item is now in the selected slot, it could still be
	 *         on its way there.
	 */
	public static boolean selectItem(int slot)
	{
		Inventory inventory = MC.player.getInventory();
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		// if the slot is negative, abort and return false
		if(slot < 0)
			return false;
		
		// if the item is already in the hotbar, just select it
		if(slot < 9)
			inventory.setSelectedSlot(slot);
		// if there is an empty slot in the hotbar, shift-click the item there
		// it will be selected in the next tick
		else if(inventory.getFreeSlot() > -1 && inventory.getFreeSlot() < 9)
			im.windowClick_QUICK_MOVE(toNetworkSlot(slot));
		// otherwise, swap with the currently selected item
		else
			im.windowClick_SWAP(toNetworkSlot(slot),
				inventory.getSelectedSlot());
		
		return true;
	}
	
	public static int toNetworkSlot(int slot)
	{
		// hotbar
		if(slot >= 0 && slot < 9)
			return slot + 36;
		
		// armor
		if(slot >= 36 && slot < 40)
			return 44 - slot;
		
		// offhand
		if(slot == 40)
			return 45;
		
		// everything else
		return slot;
	}
	
	public static boolean giveCreativeItem(Item item)
	{
		return giveCreativeItem(new ItemStack(item));
	}
	
	/**
	 * Spawns the given item stack into the first empty slot of the player's
	 * inventory in Creative Mode. If the player is not in Creative Mode or the
	 * inventory is full, this method will return <code>false</code> and do
	 * nothing.
	 */
	public static boolean giveCreativeItem(ItemStack stack)
	{
		return setCreativeStack(MC.player.getInventory().getFreeSlot(), stack);
	}
	
	public static boolean setCreativeStack(int slot, Item item)
	{
		return setCreativeStack(slot, new ItemStack(item));
	}
	
	/**
	 * Spawns/modifies/deletes the given item stack in Creative Mode. If the
	 * given slot is negative or the player is not in Creative Mode, this method
	 * will return <code>false</code> and do nothing.
	 */
	public static boolean setCreativeStack(int slot, ItemStack stack)
	{
		if(slot < 0)
			return false;
		
		if(!MC.player.hasInfiniteMaterials())
			return false;
		
		MC.player.getInventory().setItem(slot, stack);
		MC.player.connection.send(new ServerboundSetCreativeModeSlotPacket(
			toNetworkSlot(slot), stack));
		return true;
	}
}
