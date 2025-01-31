/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;

public enum PacketUtils
{
	;
	
	/**
	 * Creates a new PlayerMoveC2SPacket with modified position values. If the
	 * input packet if of a type that can't hold position data, it will be
	 * upgraded to PositionAndOnGround or Full as needed.
	 */
	public static PlayerMoveC2SPacket modifyPosition(PlayerMoveC2SPacket packet,
		double x, double y, double z)
	{
		if(packet instanceof LookAndOnGround)
			return new Full(x, y, z, packet.getYaw(0), packet.getPitch(0),
				packet.isOnGround(), packet.horizontalCollision());
		
		if(packet instanceof OnGroundOnly)
			return new PositionAndOnGround(x, y, z, packet.isOnGround(),
				packet.horizontalCollision());
		
		if(packet instanceof Full)
			return new Full(x, y, z, packet.getYaw(0), packet.getPitch(0),
				packet.isOnGround(), packet.horizontalCollision());
		
		return new PositionAndOnGround(x, y, z, packet.isOnGround(),
			packet.horizontalCollision());
	}
	
	/**
	 * Creates a new PlayerMoveC2SPacket with modified rotation values. If the
	 * input packet is of a type that can't hold rotation data, it will be
	 * upgraded to LookAndOnGround or Full as needed.
	 */
	public static PlayerMoveC2SPacket modifyRotation(PlayerMoveC2SPacket packet,
		float yaw, float pitch)
	{
		if(packet instanceof PositionAndOnGround)
			return new Full(packet.getX(0), packet.getY(0), packet.getZ(0), yaw,
				pitch, packet.isOnGround(), packet.horizontalCollision());
		
		if(packet instanceof OnGroundOnly)
			return new LookAndOnGround(yaw, pitch, packet.isOnGround(),
				packet.horizontalCollision());
		
		if(packet instanceof Full)
			return new Full(packet.getX(0), packet.getY(0), packet.getZ(0), yaw,
				pitch, packet.isOnGround(), packet.horizontalCollision());
		
		return new LookAndOnGround(yaw, pitch, packet.isOnGround(),
			packet.horizontalCollision());
	}
	
	/**
	 * Creates a new PlayerMoveC2SPacket with a modified onGround flag.
	 */
	public static PlayerMoveC2SPacket modifyOnGround(PlayerMoveC2SPacket packet,
		boolean onGround)
	{
		if(packet instanceof Full)
			return new Full(packet.getX(0), packet.getY(0), packet.getZ(0),
				packet.getYaw(0), packet.getPitch(0), onGround,
				packet.horizontalCollision());
		
		if(packet instanceof PositionAndOnGround)
			return new PositionAndOnGround(packet.getX(0), packet.getY(0),
				packet.getZ(0), onGround, packet.horizontalCollision());
		
		if(packet instanceof LookAndOnGround)
			return new LookAndOnGround(packet.getYaw(0), packet.getPitch(0),
				onGround, packet.horizontalCollision());
		
		return new OnGroundOnly(onGround, packet.horizontalCollision());
	}
	
	/**
	 * Creates a new PlayerMoveC2SPacket with a modified horizontal collision
	 * flag.
	 */
	public static PlayerMoveC2SPacket modifyHorizontalCollision(
		PlayerMoveC2SPacket packet, boolean horizontalCollision)
	{
		if(packet instanceof Full)
			return new Full(packet.getX(0), packet.getY(0), packet.getZ(0),
				packet.getYaw(0), packet.getPitch(0), packet.isOnGround(),
				horizontalCollision);
		
		if(packet instanceof PositionAndOnGround)
			return new PositionAndOnGround(packet.getX(0), packet.getY(0),
				packet.getZ(0), packet.isOnGround(), horizontalCollision);
		
		if(packet instanceof LookAndOnGround)
			return new LookAndOnGround(packet.getYaw(0), packet.getPitch(0),
				packet.isOnGround(), horizontalCollision);
		
		return new OnGroundOnly(packet.isOnGround(), horizontalCollision);
	}
}
