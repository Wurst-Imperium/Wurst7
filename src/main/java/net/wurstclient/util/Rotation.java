/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;

public record Rotation(float yaw, float pitch)
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	public void sendPlayerLookPacket()
	{
		sendPlayerLookPacket(MC.player.isOnGround());
	}
	
	public void sendPlayerLookPacket(boolean onGround)
	{
		MC.player.networkHandler.sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, onGround));
	}
	
	public double getAngleTo(Rotation other)
	{
		float yaw1 = MathHelper.wrapDegrees(yaw);
		float yaw2 = MathHelper.wrapDegrees(other.yaw);
		float diffYaw = MathHelper.wrapDegrees(yaw1 - yaw2);
		
		float pitch1 = MathHelper.wrapDegrees(pitch);
		float pitch2 = MathHelper.wrapDegrees(other.pitch);
		float diffPitch = MathHelper.wrapDegrees(pitch1 - pitch2);
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	public Vec3d toLookVec()
	{
		float radPerDeg = MathHelper.RADIANS_PER_DEGREE;
		float pi = MathHelper.PI;
		
		float adjustedYaw = -MathHelper.wrapDegrees(yaw) * radPerDeg - pi;
		float cosYaw = MathHelper.cos(adjustedYaw);
		float sinYaw = MathHelper.sin(adjustedYaw);
		
		float adjustedPitch = -MathHelper.wrapDegrees(pitch) * radPerDeg;
		float nCosPitch = -MathHelper.cos(adjustedPitch);
		float sinPitch = MathHelper.sin(adjustedPitch);
		
		return new Vec3d(sinYaw * nCosPitch, sinPitch, cosYaw * nCosPitch);
	}
	
	public static Rotation wrapped(float yaw, float pitch)
	{
		return new Rotation(MathHelper.wrapDegrees(yaw),
			MathHelper.wrapDegrees(pitch));
	}
}
