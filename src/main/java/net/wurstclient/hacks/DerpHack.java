/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Random;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"Retarded"})
public final class DerpHack extends Hack implements UpdateListener
{
	private final Random random = new Random();
	
	public DerpHack()
	{
		super("Derp", "Randomly moves your head around.\n"
			+ "Only visible to other players.");
		setCategory(Category.FUN);
	}
	
	@Override
	public void onEnable()
	{
		// disable incompatible derps
		WURST.getHax().headRollHack.setEnabled(false);
		WURST.getHax().tiredHack.setEnabled(false);
		
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
		float yaw = MC.player.yaw + random.nextFloat() * 360F - 180F;
		float pitch = random.nextFloat() * 180F - 90F;
		
		MC.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookOnly(
			yaw, pitch, MC.player.isOnGround()));
	}
}
