/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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

@SearchTags({"auto totem"})
public final class AutoTotemHack extends Hack implements UpdateListener
{
	private int nextTickSlot;
	
	public AutoTotemHack()
	{
		super("AutoTotem",
			"Automatically moves totems of undying to your off-hand.");
		setCategory(Category.COMBAT);
	}
	
	@Override
	public void onEnable()
	{
		nextTickSlot = -1;
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
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		PlayerInventory inventory = MC.player.inventory;
		
		if(nextTickSlot != -1)
		{
			im.windowClick_PICKUP(nextTickSlot);
			nextTickSlot = -1;
		}
		
		ItemStack offhandStack = inventory.getStack(40);
		if(offhandStack.getItem() == Items.TOTEM_OF_UNDYING)
			return;
		
		if(MC.currentScreen instanceof HandledScreen
			&& !(MC.currentScreen instanceof AbstractInventoryScreen))
			return;
		
		for(int slot = 0; slot <= 36; slot++)
		{
			if(inventory.getStack(slot).getItem() != Items.TOTEM_OF_UNDYING)
				continue;
			
			int newTotemSlot = slot < 9 ? slot + 36 : slot;
			boolean offhandEmpty = offhandStack.isEmpty();
			
			im.windowClick_PICKUP(newTotemSlot);
			im.windowClick_PICKUP(45);
			
			if(!offhandEmpty)
				nextTickSlot = newTotemSlot;
			
			break;
		}
	}
}
