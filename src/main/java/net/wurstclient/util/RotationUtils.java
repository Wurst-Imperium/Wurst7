/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.RotationFaker;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IClientPlayerEntity;

public enum RotationUtils
{
	;
	
	public static Vec3d getEyesPos()
	{
		ClientPlayerEntity player = WurstClient.MC.player;
		
		return new Vec3d(player.getX(),
			player.getY() + player.getEyeHeight(player.getPose()),
			player.getZ());
	}
	
	public static Vec3d getClientLookVec(float partialTicks)
	{
		ClientPlayerEntity player = WurstClient.MC.player;
		float f = 0.017453292F;
		float pi = (float)Math.PI;
		
		float f1 = MathHelper.cos(-player.getYaw(partialTicks) * f - pi);
		float f2 = MathHelper.sin(-player.getYaw(partialTicks) * f - pi);
		float f3 = -MathHelper.cos(-player.getPitch(partialTicks) * f);
		float f4 = MathHelper.sin(-player.getPitch(partialTicks) * f);
		
		return new Vec3d(f2 * f3, f4, f1 * f3);
	}
	
	public static Vec3d getServerLookVec()
	{
		RotationFaker rotationFaker = WurstClient.INSTANCE.getRotationFaker();
		float serverYaw = rotationFaker.getServerYaw();
		float serverPitch = rotationFaker.getServerPitch();
		
		float f = MathHelper.cos(-serverYaw * 0.017453292F - (float)Math.PI);
		float f1 = MathHelper.sin(-serverYaw * 0.017453292F - (float)Math.PI);
		float f2 = -MathHelper.cos(-serverPitch * 0.017453292F);
		float f3 = MathHelper.sin(-serverPitch * 0.017453292F);
		return new Vec3d(f1 * f2, f3, f * f2);
	}
	
	public static Rotation getNeededRotations(Vec3d vec)
	{
		Vec3d eyesPos = getEyesPos();
		
		double diffX = vec.x - eyesPos.x;
		double diffY = vec.y - eyesPos.y;
		double diffZ = vec.z - eyesPos.z;
		
		double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
		
		float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		float pitch = (float)-Math.toDegrees(Math.atan2(diffY, diffXZ));
		
		return Rotation.wrapped(yaw, pitch);
	}
	
	public static double getAngleToLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		
		ClientPlayerEntity player = WurstClient.MC.player;
		float currentYaw = MathHelper.wrapDegrees(player.getYaw());
		float currentPitch = MathHelper.wrapDegrees(player.getPitch());
		
		float diffYaw = MathHelper.wrapDegrees(currentYaw - needed.yaw);
		float diffPitch = MathHelper.wrapDegrees(currentPitch - needed.pitch);
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	public static double getAngleToLastReportedLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		
		IClientPlayerEntity player = WurstClient.IMC.getPlayer();
		float lastReportedYaw = MathHelper.wrapDegrees(player.getLastYaw());
		float lastReportedPitch = MathHelper.wrapDegrees(player.getLastPitch());
		
		float diffYaw = MathHelper.wrapDegrees(lastReportedYaw - needed.yaw);
		float diffPitch =
			MathHelper.wrapDegrees(lastReportedPitch - needed.pitch);
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	public static double getAngleToLastReportedLookVec(Rotation rotation)
	{
		float yaw = MathHelper.wrapDegrees(rotation.getYaw());
		float pitch = MathHelper.wrapDegrees(rotation.getPitch());
		
		IClientPlayerEntity player = WurstClient.IMC.getPlayer();
		float lastReportedYaw = MathHelper.wrapDegrees(player.getLastYaw());
		float lastReportedPitch = MathHelper.wrapDegrees(player.getLastPitch());
		
		float diffYaw = MathHelper.wrapDegrees(lastReportedYaw - yaw);
		float diffPitch = MathHelper.wrapDegrees(lastReportedPitch - pitch);
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	/**
	 * Returns true if the player is already facing within 1 degree of the
	 * specified rotation.
	 */
	public static boolean isAlreadyFacing(Rotation rotation)
	{
		return getAngleToLastReportedLookVec(rotation) <= 1.0;
	}
	
	/**
	 * Returns true if the player is facing anywhere within the given box
	 * and is no further away than the given range.
	 */
	public static boolean isFacingBox(Box box, double range)
	{
		Vec3d start = getEyesPos();
		Vec3d end = start.add(getServerLookVec().multiply(range));
		return box.raycast(start, end).isPresent();
	}
	
	public static float getHorizontalAngleToLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		return MathHelper.wrapDegrees(WurstClient.MC.player.getYaw())
			- needed.yaw;
	}
	
	/**
	 * Returns the next rotation that the player should be facing in order to
	 * slowly turn towards the specified end rotation, at a rate of roughly
	 * <code>maxChange</code> degrees per tick.
	 */
	public static Rotation slowlyTurnTowards(Rotation end, float maxChange)
	{
		Entity player = WurstClient.MC.player;
		float startYaw = player.prevYaw;
		float startPitch = player.prevPitch;
		float endYaw = end.getYaw();
		float endPitch = end.getPitch();
		
		float yawChange = Math.abs(MathHelper.wrapDegrees(endYaw - startYaw));
		float pitchChange =
			Math.abs(MathHelper.wrapDegrees(endPitch - startPitch));
		
		float maxChangeYaw =
			Math.min(maxChange, maxChange * yawChange / pitchChange);
		float maxChangePitch =
			Math.min(maxChange, maxChange * pitchChange / yawChange);
		
		float nextYaw = limitAngleChange(startYaw, endYaw, maxChangeYaw);
		float nextPitch =
			limitAngleChange(startPitch, endPitch, maxChangePitch);
		
		return new Rotation(nextYaw, nextPitch);
	}
	
	/**
	 * Limits the change in angle between the current and intended rotation to
	 * the specified maximum change. Useful for smoothing out rotations and
	 * making combat hacks harder to detect.
	 *
	 * <p>
	 * For best results, do not wrap the current angle before calling this
	 * method!
	 */
	public static float limitAngleChange(float current, float intended,
		float maxChange)
	{
		float currentWrapped = MathHelper.wrapDegrees(current);
		float intendedWrapped = MathHelper.wrapDegrees(intended);
		
		float change = MathHelper.wrapDegrees(intendedWrapped - currentWrapped);
		change = MathHelper.clamp(change, -maxChange, maxChange);
		
		return current + change;
	}
	
	/**
	 * Removes unnecessary changes in angle caused by wrapping. Useful for
	 * making combat hacks harder to detect.
	 *
	 * <p>
	 * For example, if the current angle is 179 degrees and the intended angle
	 * is -179 degrees, you only need to turn 2 degrees to face the intended
	 * angle, not 358 degrees.
	 *
	 * <p>
	 * DO NOT wrap the current angle before calling this method! You will get
	 * incorrect results if you do.
	 */
	public static float limitAngleChange(float current, float intended)
	{
		float currentWrapped = MathHelper.wrapDegrees(current);
		float intendedWrapped = MathHelper.wrapDegrees(intended);
		
		float change = MathHelper.wrapDegrees(intendedWrapped - currentWrapped);
		
		return current + change;
	}
	
	public static final class Rotation
	{
		private final float yaw;
		private final float pitch;
		
		public Rotation(float yaw, float pitch)
		{
			this.yaw = yaw;
			this.pitch = pitch;
		}
		
		public static Rotation wrapped(float yaw, float pitch)
		{
			return new Rotation(MathHelper.wrapDegrees(yaw),
				MathHelper.wrapDegrees(pitch));
		}
		
		public float getYaw()
		{
			return yaw;
		}
		
		public float getPitch()
		{
			return pitch;
		}
	}
}
