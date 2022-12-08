/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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

@SearchTags({"restock", "AutoRestock", "auto-restock", "auto restock"})
public final class RestockHack extends Hack implements UpdateListener
{
	private ItemListSetting items = new ItemListSetting("Items",
		"Item(s) to be restocked.", "minecraft:minecart");
	
	private final CheckboxSetting currentSlot = new CheckboxSetting(
		"Current slot", "Always restock in the current slot.", false);
	
	private final CheckboxSetting repairMode = new CheckboxSetting(
		"Tools repair mode", "Swap out tools that are about to break.", false);
	
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
		// check screen
		if(MC.currentScreen instanceof HandledScreen
			&& !(MC.currentScreen instanceof InventoryScreen))
			return;
		
		PlayerInventory inv = MC.player.getInventory();
		int restockSlot = currentSlot.isChecked() ? inv.selectedSlot : 0;
		for(String itemName : items.getItemNames())
		{
			ItemStack restockStack =
				MC.player.getInventory().getStack(restockSlot);
			
			boolean wrongItem =
				restockStack.isEmpty() || !itemEqual(itemName, restockStack);
			if(!wrongItem && restockStack.getCount() >= Math.min(2,
				restockStack.getMaxCount()))
				return;
			
			List<Integer> searchResult =
				searchSlotsWithItem(itemName, 0, 36, restockSlot);
			for(int itemIndex : searchResult)
			{
				int pickupIndex = itemIndex < 9 ? itemIndex + 36 : itemIndex;
				IClientPlayerInteractionManager im =
					IMC.getInteractionManager();
				im.windowClick_PICKUP(pickupIndex);
				im.windowClick_PICKUP(restockSlot + 36);
				
				if(!MC.player.playerScreenHandler.getCursorStack().isEmpty())
					im.windowClick_PICKUP(pickupIndex);
				
				if(restockStack.getCount() >= restockStack.getMaxCount())
					break;
			}
			
			if(wrongItem && searchResult.isEmpty())
				continue;
			
			break;
		}
		
		ItemStack restockStack = MC.player.getInventory().getStack(restockSlot);
		if(repairMode.isChecked() && restockStack.isDamageable()
			&& isTooDamaged(restockStack))
			for(int i = 36 - 1; i > 9 - 1; i--)
			{
				ItemStack stack = MC.player.getInventory().getStack(i);
				if(stack.isEmpty() || !stack.isDamageable())
				{
					IMC.getInteractionManager().windowClick_SWAP(i,
						restockSlot);
					break;
				}
			}
	}
	
	private boolean isTooDamaged(ItemStack stack)
	{
		return stack.getMaxDamage() - stack.getDamage() <= 4;
	}
	
	private boolean itemEqual(String itemName, ItemStack stack)
	{
		if(repairMode.isChecked() && stack.isDamageable()
			&& isTooDamaged(stack))
			return false;
		return Registries.ITEM.getId(stack.getItem()).toString()
			.equals(itemName);
	}
	
	private List<Integer> searchSlotsWithItem(String itemName, int start,
		int end, int skip)
	{
		List<Integer> slots = new LinkedList<>();
		
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
