/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto totem", "offhand", "off-hand"})
public final class AutoTotemHack extends Hack implements UpdateListener
{
	private final CheckboxSetting showCounter = new CheckboxSetting(
		"Show totem counter", "Displays the number of totems you have.", true);
	
	private final SliderSetting delay = new SliderSetting("Delay",
		"Amount of ticks to wait before equipping the next totem.", 0, 0, 20, 1,
		ValueDisplay.INTEGER);
	
	private final SliderSetting health = new SliderSetting("Health",
		"Effectively disables AutoTotem until your health reaches this value or falls below it.\n"
			+ "0 = always active",
		0, 0, 10, 0.5,
		ValueDisplay.DECIMAL.withSuffix(" hearts").withLabel(0, "ignore"));
	
	private int nextTickSlot;
	private int totems;
	private int timer;
	private boolean wasTotemInOffhand;
	
	public AutoTotemHack()
	{
		super("AutoTotem");
		setCategory(Category.COMBAT);
		addSetting(showCounter);
		addSetting(delay);
		addSetting(health);
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
		timer = 0;
		wasTotemInOffhand = false;
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
		
		PlayerInventory inventory = MC.player.getInventory();
		int nextTotemSlot = searchForTotems(inventory);
		
		ItemStack offhandStack = inventory.getStack(40);
		if(isTotem(offhandStack))
		{
			totems++;
			wasTotemInOffhand = true;
			return;
		}
		
		if(wasTotemInOffhand)
		{
			timer = delay.getValueI();
			wasTotemInOffhand = false;
		}
		
		float healthF = health.getValueF();
		if(healthF > 0 && MC.player.getHealth() > healthF * 2F)
			return;
		
		if(MC.currentScreen instanceof HandledScreen
			&& !(MC.currentScreen instanceof AbstractInventoryScreen))
			return;
		
		if(nextTotemSlot == -1)
			return;
		
		if(timer > 0)
		{
			timer--;
			return;
		}
		
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
