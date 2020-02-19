/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"jet pack"})
public final class JetpackHack extends Hack implements UpdateListener
{
	public JetpackHack()
	{
		super("Jetpack", "Allows you to fly as if you had a jetpack.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r You will take fall damage if you don't use NoFall.");
		
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().flightHack.setEnabled(false);
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
		if(MC.options.keyJump.isPressed())
			MC.player.jump();
	}
}
