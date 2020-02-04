/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IMiningToolItem;

@SearchTags({"auto sword"})
public final class AutoSwordHack extends Hack
	implements LeftClickListener, UpdateListener
{
	private int oldSlot = -1;
	private int timer;
	
	public AutoSwordHack()
	{
		super("AutoSword",
			"Automatically uses the best weapon in your hotbar to attack entities.\n"
				+ "Tip: This works with Killaura.");
		setCategory(Category.COMBAT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(LeftClickListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(LeftClickListener.class, this);
		
		// reset slot
		if(oldSlot != -1)
		{
			MC.player.inventory.selectedSlot = oldSlot;
			oldSlot = -1;
		}
	}
	
	@Override
	public void onUpdate()
	{
		// update timer
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		// reset slot
		if(oldSlot != -1)
		{
			MC.player.inventory.selectedSlot = oldSlot;
			oldSlot = -1;
		}
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		// check hitResult
		if(MC.crosshairTarget == null
			|| MC.crosshairTarget.getType() != HitResult.Type.ENTITY)
			return;
		
		setSlot();
	}
	
	public void setSlot()
	{
		// check if active
		if(!isEnabled())
			return;
		
		// wait for AutoEat
		if(WURST.getHax().autoEatHack.isEating())
			return;
		
		// find best weapon
		float bestDamage = 0;
		int bestSlot = -1;
		for(int i = 0; i < 9; i++)
		{
			// skip empty slots
			if(MC.player.inventory.getInvStack(i).isEmpty())
				continue;
			
			Item item = MC.player.inventory.getInvStack(i).getItem();
			
			// get damage
			float damage = 0;
			if(item instanceof SwordItem)
				damage = ((SwordItem)item).getAttackDamage();
			else if(item instanceof MiningToolItem)
				damage = ((IMiningToolItem)item).getAttackDamage();
			
			// compare with previous best weapon
			if(damage > bestDamage)
			{
				bestDamage = damage;
				bestSlot = i;
			}
		}
		
		// check if any weapon was found
		if(bestSlot == -1)
			return;
		
		// save old slot
		if(oldSlot == -1)
			oldSlot = MC.player.inventory.selectedSlot;
		
		// set slot
		MC.player.inventory.selectedSlot = bestSlot;
		
		// start timer
		timer = 4;
		EVENTS.add(UpdateListener.class, this);
	}
}
