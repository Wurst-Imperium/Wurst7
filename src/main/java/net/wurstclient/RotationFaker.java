/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.util.RotationUtils;

public final class RotationFaker
	implements PreMotionListener, PostMotionListener
{
	private boolean fakeRotation;
	private float serverYaw;
	private float serverPitch;
	private float realYaw;
	private float realPitch;
	
	@Override
	public void onPreMotion()
	{
		if(!fakeRotation)
			return;
		
		ClientPlayerEntity player = WurstClient.MC.player;
		realYaw = player.yaw;
		realPitch = player.pitch;
		player.yaw = serverYaw;
		player.pitch = serverPitch;
	}
	
	@Override
	public void onPostMotion()
	{
		if(!fakeRotation)
			return;
		
		ClientPlayerEntity player = WurstClient.MC.player;
		player.yaw = realYaw;
		player.pitch = realPitch;
		fakeRotation = false;
	}
	
	public void faceVectorPacket(Vec3d vec)
	{
		RotationUtils.Rotation rotations =
			RotationUtils.getNeededRotations(vec);
		
		fakeRotation = true;
		serverYaw = rotations.getYaw();
		serverPitch = rotations.getPitch();
	}
	
	public void faceVectorClient(Vec3d vec)
	{
		RotationUtils.Rotation rotations =
			RotationUtils.getNeededRotations(vec);
		
		WurstClient.MC.player.yaw = rotations.getYaw();
		WurstClient.MC.player.pitch = rotations.getPitch();
	}
	
	public void faceVectorClientIgnorePitch(Vec3d vec)
	{
		RotationUtils.Rotation rotations =
			RotationUtils.getNeededRotations(vec);
		
		WurstClient.MC.player.yaw = rotations.getYaw();
		WurstClient.MC.player.pitch = 0;
	}
	
	public float getServerYaw()
	{
		return fakeRotation ? serverYaw : WurstClient.MC.player.yaw;
	}
	
	public float getServerPitch()
	{
		return fakeRotation ? serverPitch : WurstClient.MC.player.pitch;
	}
}
