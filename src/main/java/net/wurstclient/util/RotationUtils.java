/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.network.ClientPlayerEntity;
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
	
	public static Vec3d getClientLookVec()
	{
		ClientPlayerEntity player = WurstClient.MC.player;
		float f = 0.017453292F;
		float pi = (float)Math.PI;
		
		float f1 = MathHelper.cos(-player.yaw * f - pi);
		float f2 = MathHelper.sin(-player.yaw * f - pi);
		float f3 = -MathHelper.cos(-player.pitch * f);
		float f4 = MathHelper.sin(-player.pitch * f);
		
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
		
		return new Rotation(yaw, pitch);
	}
	
	public static double getAngleToLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		
		ClientPlayerEntity player = WurstClient.MC.player;
		float currentYaw = MathHelper.wrapDegrees(player.yaw);
		float currentPitch = MathHelper.wrapDegrees(player.pitch);
		
		float diffYaw = currentYaw - needed.yaw;
		float diffPitch = currentPitch - needed.pitch;
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	public static double getAngleToLastReportedLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		
		IClientPlayerEntity player = WurstClient.IMC.getPlayer();
		float lastReportedYaw = MathHelper.wrapDegrees(player.getLastYaw());
		float lastReportedPitch = MathHelper.wrapDegrees(player.getLastPitch());
		
		float diffYaw = lastReportedYaw - needed.yaw;
		float diffPitch = lastReportedPitch - needed.pitch;
		
		return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
	}
	
	public static float getHorizontalAngleToLookVec(Vec3d vec)
	{
		Rotation needed = getNeededRotations(vec);
		return MathHelper.wrapDegrees(WurstClient.MC.player.yaw) - needed.yaw;
	}
	
	public static final class Rotation
	{
		private final float yaw;
		private final float pitch;
		
		public Rotation(float yaw, float pitch)
		{
			this.yaw = MathHelper.wrapDegrees(yaw);
			this.pitch = MathHelper.wrapDegrees(pitch);
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
