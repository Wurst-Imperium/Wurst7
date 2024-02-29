/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.hack.Hack;

@SearchTags({"anti hunger"})
public final class AntiHungerHack extends Hack implements PacketOutputListener
{
	public AntiHungerHack()
	{
		super("AntiHunger");
		setCategory(Category.MOVEMENT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(!(event.getPacket() instanceof PlayerMoveC2SPacket oldPacket))
			return;
		
		if(!MC.player.isOnGround() || MC.player.fallDistance > 0.5)
			return;
		
		if(MC.interactionManager.isBreakingBlock())
			return;
		
		double x = oldPacket.getX(-1);
		double y = oldPacket.getY(-1);
		double z = oldPacket.getZ(-1);
		float yaw = oldPacket.getYaw(-1);
		float pitch = oldPacket.getPitch(-1);
		
		Packet<?> newPacket;
		if(oldPacket.changesPosition())
			if(oldPacket.changesLook())
				newPacket =
					new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false);
			else
				newPacket =
					new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false);
		else if(oldPacket.changesLook())
			newPacket =
				new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, false);
		else
			newPacket = new PlayerMoveC2SPacket.OnGroundOnly(false);
		
		event.setPacket(newPacket);
	}
}
