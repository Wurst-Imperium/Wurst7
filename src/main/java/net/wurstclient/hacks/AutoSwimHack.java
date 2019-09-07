/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"auto swim"})
public final class AutoSwimHack extends Hack implements UpdateListener
{
	public AutoSwimHack()
	{
		super("AutoSwim", "Triggers the swimming animation automatically.");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getEventManager().add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		if(player.horizontalCollision || player.isSneaking())
			return;
		
		if(!player.isInsideWater())
			return;
		
		if(player.forwardSpeed > 0)
			player.setSprinting(true);
	}
}
