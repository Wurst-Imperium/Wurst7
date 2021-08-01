/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"creative flight", "CreativeFly", "creative fly"})
public final class CreativeFlightHack extends Hack implements UpdateListener
{
	public CreativeFlightHack()
	{
		super("CreativeFlight",
			"Allows you to you fly like in Creative Mode.\n\n"
				+ "\u00a7c\u00a7lWARNING:\u00a7r"
				+ " You will take fall damage if you don't use NoFall.");
		
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().jetpackHack.setEnabled(false);
		WURST.getHax().flightHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		
		ClientPlayerEntity player = MC.player;
		PlayerAbilities abilities = player.getAbilities();
		
		boolean creative = player.isCreative();
		abilities.flying = creative && !player.isOnGround();
		abilities.allowFlying = creative;
	}
	
	@Override
	public void onUpdate()
	{
		MC.player.getAbilities().allowFlying = true;
	}
}
