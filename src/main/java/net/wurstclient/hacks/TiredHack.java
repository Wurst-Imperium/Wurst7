/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.Rotation;

public final class TiredHack extends Hack implements UpdateListener
{
	public TiredHack()
	{
		super("Tired");
		setCategory(Category.FUN);
	}
	
	@Override
	public void onEnable()
	{
		// disable incompatible derps
		WURST.getHax().derpHack.setEnabled(false);
		WURST.getHax().headRollHack.setEnabled(false);
		
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
		new Rotation(MC.player.getYaw(), MC.player.age % 100)
			.sendPlayerLookPacket();
	}
}
