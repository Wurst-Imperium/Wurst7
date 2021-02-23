/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.player.PlayerInventory;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"auto switch"})
public final class AutoSwitchHack extends Hack implements UpdateListener
{
	public AutoSwitchHack()
	{
		super("AutoSwitch", "Switches the item in your hand all the time.\n\n"
			+ "\u00a7lProTip:\u00a7r Use this in combination with BuildRandom while\n"
			+ "having a lot of different colored wool or concrete\n"
			+ "blocks in your hotbar.");
		setCategory(Category.ITEMS);
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
		PlayerInventory inventory = MC.player.inventory;
		
		if(inventory.selectedSlot == 8)
			inventory.selectedSlot = 0;
		else
			inventory.selectedSlot++;
	}
}
