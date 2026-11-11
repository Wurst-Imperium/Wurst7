/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

public record Rotation(float yaw, float pitch)
{
	private static final Minecraft MC = WurstClient.MC;
	
	public void applyToClientPlayer()
	{
		float adjustedYaw =
			RotationUtils.limitAngleChange(MC.player.getYRot(), yaw);
		MC.player.setYRot(adjustedYaw);
		MC.player.setXRot(pitch);
	}
	
	public void sendPlayerLookPacket()
	{
		sendPlayerLookPacket(MC.player.onGround(),
			MC.player.horizontalCollision);
	}
	
	public void sendPlayerLookPacket(boolean onGround,
		boolean horizontalCollision)
	{
		MC.player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw,
			pitch, onGround, horizontalCollision));
	}
	
	public double getAngleTo(Rotation other)
	{
		Vec3 lookVec = toLookVec();
		Vec3 otherLookVec = other.toLookVec();
		return Math.toDegrees(Math.atan2(lookVec.cross(otherLookVec).length(),
			lookVec.dot(otherLookVec)));
	}
	
	public double getYawPitchDistanceTo(Rotation other)
	{
		float yaw1 = Mth.wrapDegrees(yaw);
		float yaw2 = Mth.wrapDegrees(other.yaw);
		float diffYaw = Mth.wrapDegrees(yaw1 - yaw2);
		
		float pitch1 = Mth.wrapDegrees(pitch);
		float pitch2 = Mth.wrapDegrees(other.pitch);
		float diffPitch = Mth.wrapDegrees(pitch1 - pitch2);
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	public Rotation slowlyTurnTowards(Rotation end, float maxChange)
	{
		float yawChange = Math.abs(Mth.wrapDegrees(end.yaw - yaw));
		float pitchChange = Math.abs(Mth.wrapDegrees(end.pitch - pitch));
		
		float maxChangeYaw = pitchChange == 0 ? maxChange
			: Math.min(maxChange, maxChange * yawChange / pitchChange);
		float maxChangePitch = yawChange == 0 ? maxChange
			: Math.min(maxChange, maxChange * pitchChange / yawChange);
		
		float nextYaw =
			RotationUtils.limitAngleChange(yaw, end.yaw, maxChangeYaw);
		float nextPitch =
			RotationUtils.limitAngleChange(pitch, end.pitch, maxChangePitch);
		
		return new Rotation(nextYaw, nextPitch);
	}
	
	public Rotation withYaw(float yaw)
	{
		return new Rotation(yaw, pitch);
	}
	
	public Rotation withPitch(float pitch)
	{
		return new Rotation(yaw, pitch);
	}
	
	public Vec3 toLookVec()
	{
		double yawRad = Math.toRadians(Mth.wrapDegrees(yaw));
		double cosYaw = Math.cos(yawRad);
		double sinYaw = Math.sin(yawRad);
		
		double pitchRad = Math.toRadians(Mth.wrapDegrees(pitch));
		double cosPitch = Math.cos(pitchRad);
		double sinPitch = Math.sin(pitchRad);
		
		return new Vec3(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
	}
	
	public Quaternionf toQuaternion()
	{
		double yawRad = -Math.toRadians(Mth.wrapDegrees(yaw));
		double pitchRad = Math.toRadians(Mth.wrapDegrees(pitch));
		
		double sinYaw = Math.sin(yawRad / 2);
		double cosYaw = Math.cos(yawRad / 2);
		double sinPitch = Math.sin(pitchRad / 2);
		double cosPitch = Math.cos(pitchRad / 2);
		
		double x = sinPitch * cosYaw;
		double y = cosPitch * sinYaw;
		double z = -sinPitch * sinYaw;
		double w = cosPitch * cosYaw;
		
		return new Quaternionf((float)x, (float)y, (float)z, (float)w);
	}
	
	public static Rotation wrapped(float yaw, float pitch)
	{
		return new Rotation(Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch));
	}
}
