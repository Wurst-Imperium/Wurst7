/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public enum PacketUtils
{
	;
	
	/**
	 * Creates a new PlayerMoveC2SPacket with modified position values. If the
	 * input packet if of a type that can't hold position data, it will be
	 * upgraded to PositionAndOnGround or Full as needed.
	 */
	public static ServerboundMovePlayerPacket modifyPosition(
		ServerboundMovePlayerPacket packet, double x, double y, double z)
	{
		if(packet instanceof Rot)
			return new PosRot(x, y, z, packet.getYRot(0), packet.getXRot(0),
				packet.isOnGround(), packet.horizontalCollision());
		
		if(packet instanceof StatusOnly)
			return new Pos(x, y, z, packet.isOnGround(),
				packet.horizontalCollision());
		
		if(packet instanceof PosRot)
			return new PosRot(x, y, z, packet.getYRot(0), packet.getXRot(0),
				packet.isOnGround(), packet.horizontalCollision());
		
		return new Pos(x, y, z, packet.isOnGround(),
			packet.horizontalCollision());
	}
	
	/**
	 * Creates a new PlayerMoveC2SPacket with modified rotation values. If the
	 * input packet is of a type that can't hold rotation data, it will be
	 * upgraded to LookAndOnGround or Full as needed.
	 */
	public static ServerboundMovePlayerPacket modifyRotation(
		ServerboundMovePlayerPacket packet, float yaw, float pitch)
	{
		if(packet instanceof Pos)
			return new PosRot(packet.getX(0), packet.getY(0), packet.getZ(0),
				yaw, pitch, packet.isOnGround(), packet.horizontalCollision());
		
		if(packet instanceof StatusOnly)
			return new Rot(yaw, pitch, packet.isOnGround(),
				packet.horizontalCollision());
		
		if(packet instanceof PosRot)
			return new PosRot(packet.getX(0), packet.getY(0), packet.getZ(0),
				yaw, pitch, packet.isOnGround(), packet.horizontalCollision());
		
		return new Rot(yaw, pitch, packet.isOnGround(),
			packet.horizontalCollision());
	}
	
	/**
	 * Creates a new PlayerMoveC2SPacket with a modified onGround flag.
	 */
	public static ServerboundMovePlayerPacket modifyOnGround(
		ServerboundMovePlayerPacket packet, boolean onGround)
	{
		if(packet instanceof PosRot)
			return new PosRot(packet.getX(0), packet.getY(0), packet.getZ(0),
				packet.getYRot(0), packet.getXRot(0), onGround,
				packet.horizontalCollision());
		
		if(packet instanceof Pos)
			return new Pos(packet.getX(0), packet.getY(0), packet.getZ(0),
				onGround, packet.horizontalCollision());
		
		if(packet instanceof Rot)
			return new Rot(packet.getYRot(0), packet.getXRot(0), onGround,
				packet.horizontalCollision());
		
		return new StatusOnly(onGround, packet.horizontalCollision());
	}
	
	/**
	 * Creates a new PlayerMoveC2SPacket with a modified horizontal collision
	 * flag.
	 */
	public static ServerboundMovePlayerPacket modifyHorizontalCollision(
		ServerboundMovePlayerPacket packet, boolean horizontalCollision)
	{
		if(packet instanceof PosRot)
			return new PosRot(packet.getX(0), packet.getY(0), packet.getZ(0),
				packet.getYRot(0), packet.getXRot(0), packet.isOnGround(),
				horizontalCollision);
		
		if(packet instanceof Pos)
			return new Pos(packet.getX(0), packet.getY(0), packet.getZ(0),
				packet.isOnGround(), horizontalCollision);
		
		if(packet instanceof Rot)
			return new Rot(packet.getYRot(0), packet.getXRot(0),
				packet.isOnGround(), horizontalCollision);
		
		return new StatusOnly(packet.isOnGround(), horizontalCollision);
	}
}
