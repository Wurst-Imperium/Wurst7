/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"auto totem"})
public final class AutoTotemHack extends Hack implements UpdateListener
{
	private final CheckboxSetting showCounter = new CheckboxSetting(
		"Show totem counter", "Displays the number of totems you have.", true);
	
	private int nextTickSlot;
	private int totems;
	
	public AutoTotemHack()
	{
		super("AutoTotem",
			"Automatically moves totems of undying to your off-hand.");
		setCategory(Category.COMBAT);
		addSetting(showCounter);
	}
	
	@Override
	public String getRenderName()
	{
		if(!showCounter.isChecked())
			return getName();
		
		switch(totems)
		{
			case 1:
			return getName() + " [1 totem]";
			
			default:
			return getName() + " [" + totems + " totems]";
		}
	}
	
	@Override
	public void onEnable()
	{
		nextTickSlot = -1;
		totems = 0;
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
		finishMovingTotem();
		
		PlayerInventory inventory = MC.player.inventory;
		int nextTotemSlot = searchForTotems(inventory);
		
		ItemStack offhandStack = inventory.getStack(40);
		if(isTotem(offhandStack))
		{
			totems++;
			return;
		}
		
		if(MC.currentScreen instanceof HandledScreen
			&& !(MC.currentScreen instanceof AbstractInventoryScreen))
			return;
		
		if(nextTotemSlot != -1)
			moveTotem(nextTotemSlot, offhandStack);
	}
	
	private void moveTotem(int nextTotemSlot, ItemStack offhandStack)
	{
		boolean offhandEmpty = offhandStack.isEmpty();
		
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		im.windowClick_PICKUP(nextTotemSlot);
		im.windowClick_PICKUP(45);
		
		if(!offhandEmpty)
			nextTickSlot = nextTotemSlot;
	}
	
	private void finishMovingTotem()
	{
		if(nextTickSlot == -1)
			return;
		
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		im.windowClick_PICKUP(nextTickSlot);
		nextTickSlot = -1;
	}
	
	private int searchForTotems(PlayerInventory inventory)
	{
		totems = 0;
		int nextTotemSlot = -1;
		
		for(int slot = 0; slot <= 36; slot++)
		{
			if(!isTotem(inventory.getStack(slot)))
				continue;
			
			totems++;
			
			if(nextTotemSlot == -1)
				nextTotemSlot = slot < 9 ? slot + 36 : slot;
		}
		
		return nextTotemSlot;
	}
	
	private boolean isTotem(ItemStack stack)
	{
		return stack.getItem() == Items.TOTEM_OF_UNDYING;
	}
}
