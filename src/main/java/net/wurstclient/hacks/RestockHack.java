/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"restock", "AutoRestock", "auto-restock", "auto restock"})
public final class RestockHack extends Hack implements UpdateListener
{
	private ItemListSetting items = new ItemListSetting("Items",
		"Item(s) to be restocked.", "minecraft:minecart");
	
	// TODO: Replace this checkbox with a "Restock Slot" slider. 1-9 correspond
	// to their slots, while 0 (default) sets the slider to "auto" and behaves
	// the same as this checkbox.
	private final CheckboxSetting currentSlot = new CheckboxSetting(
		"Current slot", "Always restock in the current slot.", false);
	
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
		addSetting(currentSlot);
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
		int hotbarSlot = currentSlot.isChecked() ? inv.selectedSlot : 0;
		for(String itemName : items.getItemNames())
		{
			ItemStack hotbarStack =
				MC.player.getInventory().getStack(hotbarSlot);
			
			boolean wrongItem =
				hotbarStack.isEmpty() || !itemEqual(itemName, hotbarStack);
			// TODO: Add a "Restock Threshold" slider for the number 2 here.
			// Default should be changed to 1.
			if(!wrongItem && hotbarStack.getCount() >= Math.min(2,
				hotbarStack.getMaxCount()))
				return;
			
			List<Integer> searchResult =
				searchSlotsWithItem(itemName, 0, 36, hotbarSlot);
			for(int itemIndex : searchResult)
			{
				int pickupIndex = itemIndex < 9 ? itemIndex + 36 : itemIndex;
				IClientPlayerInteractionManager im =
					IMC.getInteractionManager();
				im.windowClick_PICKUP(pickupIndex);
				im.windowClick_PICKUP(hotbarSlot + 36);
				
				if(!MC.player.playerScreenHandler.getCursorStack().isEmpty())
					im.windowClick_PICKUP(pickupIndex);
				
				if(hotbarStack.getCount() >= hotbarStack.getMaxCount())
					break;
			}
			
			if(wrongItem && searchResult.isEmpty())
				continue;
			
			break;
		}
		
		ItemStack restockStack = MC.player.getInventory().getStack(hotbarSlot);
		if(repairMode.getValueI() > 0 && restockStack.isDamageable()
			&& isTooDamaged(restockStack))
			for(int i = 36 - 1; i > 9 - 1; i--)
			{
				ItemStack stack = MC.player.getInventory().getStack(i);
				if(stack.isEmpty() || !stack.isDamageable())
				{
					IMC.getInteractionManager().windowClick_SWAP(i, hotbarSlot);
					break;
				}
			}
	}
	
	private boolean itemEqual(String itemName, ItemStack stack)
	{
		if(repairMode.getValueI() > 0 && stack.isDamageable()
			&& isTooDamaged(stack))
			return false;
		
		return Registries.ITEM.getId(stack.getItem()).toString()
			.equals(itemName);
	}
	
	private boolean isTooDamaged(ItemStack stack)
	{
		return stack.getMaxDamage() - stack.getDamage() <= repairMode
			.getValueI();
	}
	
	private List<Integer> searchSlotsWithItem(String itemName, int start,
		int end, int skip)
	{
		List<Integer> slots = new ArrayList<>();
		
		for(int i = start; i < end; i++)
		{
			if(i == skip)
				continue;
			
			ItemStack stack = MC.player.getInventory().getStack(i);
			if(stack.isEmpty())
				continue;
			
			if(itemEqual(itemName, stack))
				slots.add(i);
		}
		
		return slots;
	}
}
