/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

@SearchTags({"jet pack", "AirJump", "air jump"})
public final class JetpackHack extends Hack implements UpdateListener
{
	public JetpackHack()
	{
		super("Jetpack");
		
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().creativeFlightHack.setEnabled(false);
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
		if(MC.options.jumpKey.isPressed())
			MC.player.jump();
	}
}
