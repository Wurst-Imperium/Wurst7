/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"AutoRestock", "auto-restock", "auto restock"})
public final class RestockHack extends Hack implements UpdateListener
{
	public static final int OFFHAND_ID = PlayerInventory.OFF_HAND_SLOT;
	public static final int OFFHAND_PKT_ID = 45;
	
	private static final List<Integer> SEARCH_SLOTS =
		Stream.concat(IntStream.range(0, 36).boxed(), Stream.of(OFFHAND_ID))
			.collect(Collectors.toCollection(ArrayList::new));
	
	private ItemListSetting items = new ItemListSetting("Items",
		"Item(s) to be restocked.", "minecraft:minecart");
	
	private final SliderSetting restockSlot = new SliderSetting("Slot",
		"To which slot should we restock.", 0, -1, 9, 1,
		ValueDisplay.INTEGER.withLabel(9, "offhand").withLabel(-1, "current"));
	
	private final SliderSetting restockAmount = new SliderSetting(
		"Minimum amount",
		"Minimum amount of items in hand before a new round of restocking is triggered.",
		1, 1, 64, 1, ValueDisplay.INTEGER);
	
	private final SliderSetting repairMode = new SliderSetting(
		"Tools repair mode",
		"Swaps out tools when their durability reaches the given threshold, so you can repair them before they break.\n"
			+ "Can be adjusted from 0 (off) to 100.",
		0, 0, 100, 1, ValueDisplay.INTEGER.withLabel(0, "off"));
	
	public RestockHack()
	{
		super("Restock");
		setCategory(Category.ITEMS);
		addSetting(items);
		addSetting(restockSlot);
		addSetting(restockAmount);
		addSetting(repairMode);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// Don't mess with the inventory while it's open.
		if(MC.currentScreen instanceof HandledScreen)
			return;
		
		PlayerInventory inv = MC.player.getInventory();
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		int hotbarSlot = restockSlot.getValueI();
		if(hotbarSlot == -1)
			hotbarSlot = inv.selectedSlot;
		else if(hotbarSlot == 9)
			hotbarSlot = OFFHAND_ID;
		
		for(String itemName : items.getItemNames())
		{
			ItemStack hotbarStack = inv.getStack(hotbarSlot);
			
			boolean wrongItem =
				hotbarStack.isEmpty() || !itemEqual(itemName, hotbarStack);
			if(!wrongItem && hotbarStack.getCount() >= Math
				.min(restockAmount.getValueI(), hotbarStack.getMaxCount()))
				return;
			
			List<Integer> searchResult =
				searchSlotsWithItem(itemName, hotbarSlot);
			for(int itemIndex : searchResult)
			{
				int pickupIndex = dataSlotToNetworkSlot(itemIndex);
				
				im.windowClick_PICKUP(pickupIndex);
				im.windowClick_PICKUP(dataSlotToNetworkSlot(hotbarSlot));
				if(!MC.player.playerScreenHandler.getCursorStack().isEmpty())
					im.windowClick_PICKUP(pickupIndex);
				
				if(hotbarStack.getCount() >= hotbarStack.getMaxCount())
					break;
			}
			
			if(wrongItem && searchResult.isEmpty())
				continue;
			
			break;
		}
		
		ItemStack restockStack = inv.getStack(hotbarSlot);
		if(repairMode.getValueI() > 0 && restockStack.isDamageable()
			&& isTooDamaged(restockStack))
			for(int i : SEARCH_SLOTS)
			{
				if(i == hotbarSlot || i == OFFHAND_ID)
					continue;
				
				ItemStack stack = inv.getStack(i);
				if(stack.isEmpty() || !stack.isDamageable())
				{
					IMC.getInteractionManager().windowClick_SWAP(i,
						dataSlotToNetworkSlot(hotbarSlot));
					break;
				}
			}
	}
	
	private boolean isTooDamaged(ItemStack stack)
	{
		return stack.getMaxDamage() - stack.getDamage() <= repairMode
			.getValueI();
	}
	
	private int dataSlotToNetworkSlot(int index)
	{
		// hotbar
		if(index >= 0 && index <= 8)
			return index + 36;
		
		// main inventory
		if(index >= 9 && index <= 35)
			return index;
		
		if(index == OFFHAND_ID)
			return OFFHAND_PKT_ID;
		
		throw new IllegalArgumentException("unimplemented data slot");
	}
	
	private List<Integer> searchSlotsWithItem(String itemName, int slotToSkip)
	{
		List<Integer> slots = new ArrayList<>();
		
		for(int i : SEARCH_SLOTS)
		{
			if(i == slotToSkip)
				continue;
			
			ItemStack stack = MC.player.getInventory().getStack(i);
			if(stack.isEmpty())
				continue;
			
			if(itemEqual(itemName, stack))
				slots.add(i);
		}
		
		return slots;
	}
	
	private boolean itemEqual(String itemName, ItemStack stack)
	{
		if(repairMode.getValueI() > 0 && stack.isDamageable()
			&& isTooDamaged(stack))
			return false;
		
		return Registries.ITEM.getId(stack.getItem()).toString()
			.equals(itemName);
	}
}
