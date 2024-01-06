/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.RotationFaker;
import net.wurstclient.WurstClient;

public enum RotationUtils
{
	;
	
	private static final MinecraftClient MC = WurstClient.MC;
	
	public static Vec3d getEyesPos()
	{
		ClientPlayerEntity player = MC.player;
		float eyeHeight = player.getEyeHeight(player.getPose());
		return player.getPos().add(0, eyeHeight, 0);
	}
	
	public static Vec3d getClientLookVec(float partialTicks)
	{
		float yaw = MC.player.getYaw(partialTicks);
		float pitch = MC.player.getPitch(partialTicks);
		return new Rotation(yaw, pitch).toLookVec();
	}
	
	public static Vec3d getServerLookVec()
	{
		RotationFaker rf = WurstClient.INSTANCE.getRotationFaker();
		return new Rotation(rf.getServerYaw(), rf.getServerPitch()).toLookVec();
	}
	
	public static Rotation getNeededRotations(Vec3d vec)
	{
		Vec3d eyes = getEyesPos();
		
		double diffX = vec.x - eyes.x;
		double diffZ = vec.z - eyes.z;
		double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		
		double diffY = vec.y - eyes.y;
		double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
		double pitch = -Math.toDegrees(Math.atan2(diffY, diffXZ));
		
		return Rotation.wrapped((float)yaw, (float)pitch);
	}
	
	public static double getAngleToLookVec(Vec3d vec)
	{
		ClientPlayerEntity player = MC.player;
		Rotation current = new Rotation(player.getYaw(), player.getPitch());
		Rotation needed = getNeededRotations(vec);
		return current.getAngleTo(needed);
	}
	
	public static float getHorizontalAngleToLookVec(Vec3d vec)
	{
		float currentYaw = MathHelper.wrapDegrees(MC.player.getYaw());
		float neededYaw = getNeededRotations(vec).yaw();
		return MathHelper.wrapDegrees(currentYaw - neededYaw);
	}
	
	/**
	 * Returns true if the player is already facing within 1 degree of the
	 * specified rotation.
	 */
	public static boolean isAlreadyFacing(Rotation rotation)
	{
		return getAngleToLastReportedLookVec(rotation) <= 1.0;
	}
	
	public static double getAngleToLastReportedLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		return getAngleToLastReportedLookVec(needed);
	}
	
	public static double getAngleToLastReportedLookVec(Rotation rotation)
	{
		ClientPlayerEntity player = MC.player;
		Rotation lastReported = new Rotation(player.lastYaw, player.lastPitch);
		return lastReported.getAngleTo(rotation);
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
	
	/**
	 * Returns the next rotation that the player should be facing in order to
	 * slowly turn towards the specified end rotation, at a rate of roughly
	 * <code>maxChange</code> degrees per tick.
	 */
	public static Rotation slowlyTurnTowards(Rotation end, float maxChange)
	{
		float startYaw = MC.player.prevYaw;
		float startPitch = MC.player.prevPitch;
		float endYaw = end.yaw();
		float endPitch = end.pitch();
		
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
}
