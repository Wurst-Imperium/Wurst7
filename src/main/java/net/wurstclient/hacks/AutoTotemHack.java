/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.InventoryUtils;

@SearchTags({"auto totem", "offhand", "off-hand"})
public final class AutoTotemHack extends Hack implements UpdateListener
{
	private final CheckboxSetting showCounter = new CheckboxSetting(
		"Show totem counter", "Displays the number of totems you have.", true);
	
	private final SliderSetting delay = new SliderSetting("Delay",
		"Amount of ticks to wait before equipping the next totem.", 0, 0, 20, 1,
		ValueDisplay.INTEGER);
	
	private final SliderSetting health = new SliderSetting("Health",
		"Won't equip a totem until your health reaches this value or falls"
			+ " below it.\n" + "0 = always active",
		0, 0, 10, 0.5, ValueDisplay.DECIMAL.withSuffix(" hearts")
			.withLabel(1, "1 heart").withLabel(0, "ignore"));
	
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
		
		if(totems == 1)
			return getName() + " [1 totem]";
		
		return getName() + " [" + totems + " totems]";
	}
	
	@Override
	protected void onEnable()
	{
		nextTickSlot = -1;
		totems = 0;
		timer = 0;
		wasTotemInOffhand = false;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		finishMovingTotem();
		
		int nextTotemSlot = searchForTotems();
		
		if(isTotem(MC.player.getOffhandItem()))
		{
			wasTotemInOffhand = true;
			return;
		}
		
		if(wasTotemInOffhand)
		{
			timer = delay.getValueI();
			wasTotemInOffhand = false;
		}
		
		if(nextTotemSlot == -1)
			return;
		
		float healthF = health.getValueF();
		if(healthF > 0 && MC.player.getHealth() > healthF * 2F)
			return;
		
		// don't move items while a container is open
		if(MC.screen instanceof AbstractContainerScreen
			&& !(MC.screen instanceof InventoryScreen
				|| MC.screen instanceof CreativeModeInventoryScreen))
			return;
		
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		moveToOffhand(nextTotemSlot);
	}
	
	private void moveToOffhand(int itemSlot)
	{
		boolean offhandEmpty = MC.player.getOffhandItem().isEmpty();
		
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		im.windowClick_PICKUP(itemSlot);
		im.windowClick_PICKUP(45);
		
		if(!offhandEmpty)
			nextTickSlot = itemSlot;
	}
	
	private void finishMovingTotem()
	{
		if(nextTickSlot == -1)
			return;
		
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		im.windowClick_PICKUP(nextTickSlot);
		nextTickSlot = -1;
	}
	
	private int searchForTotems()
	{
		totems = InventoryUtils.count(this::isTotem, 40, true);
		if(totems <= 0)
			return -1;
		
		int totemSlot = InventoryUtils.indexOf(this::isTotem, 40);
		return InventoryUtils.toNetworkSlot(totemSlot);
	}
	
	private boolean isTotem(ItemStack stack)
	{
		return stack.is(Items.TOTEM_OF_UNDYING);
	}
}
