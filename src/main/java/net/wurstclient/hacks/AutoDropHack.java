/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ItemListSetting;

@SearchTags({"auto drop", "AutoEject", "auto-eject", "auto eject",
	"InventoryCleaner", "inventory cleaner", "InvCleaner", "inv cleaner"})
public final class AutoDropHack extends Hack implements UpdateListener
{
	private ItemListSetting items = new ItemListSetting("Items",
		"Unwanted items that will be dropped.", "minecraft:allium",
		"minecraft:azure_bluet", "minecraft:blue_orchid",
		"minecraft:cornflower", "minecraft:dandelion", "minecraft:lilac",
		"minecraft:lily_of_the_valley", "minecraft:orange_tulip",
		"minecraft:oxeye_daisy", "minecraft:peony", "minecraft:pink_tulip",
		"minecraft:poisonous_potato", "minecraft:poppy", "minecraft:red_tulip",
		"minecraft:rose_bush", "minecraft:rotten_flesh", "minecraft:sunflower",
		"minecraft:wheat_seeds", "minecraft:white_tulip");
	
	private final String renderName =
		Math.random() < 0.01 ? "AutoLinus" : getName();
	
	public AutoDropHack()
	{
		super("AutoDrop", "Automatically drops unwanted items.");
		setCategory(Category.ITEMS);
		addSetting(items);
	}
	
	@Override
	public String getRenderName()
	{
		return renderName;
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
		for(int slot = 9; slot < 45; slot++)
		{
			int adjustedSlot = slot;
			if(adjustedSlot >= 36)
				adjustedSlot -= 36;
			ItemStack stack = MC.player.inventory.getInvStack(adjustedSlot);
			
			if(stack.isEmpty())
				continue;
			
			Item item = stack.getItem();
			String itemName = Registry.ITEM.getId(item).toString();
			
			if(!items.getItemNames().contains(itemName))
				continue;
			
			IMC.getInteractionManager().windowClick_THROW(slot);
		}
	}
}
