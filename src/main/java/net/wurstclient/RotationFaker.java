/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.util.Rotation;
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
		
		LocalPlayer player = WurstClient.MC.player;
		realYaw = player.getYRot();
		realPitch = player.getXRot();
		player.setYRot(serverYaw);
		player.setXRot(serverPitch);
	}
	
	@Override
	public void onPostMotion()
	{
		if(!fakeRotation)
			return;
		
		LocalPlayer player = WurstClient.MC.player;
		player.setYRot(realYaw);
		player.setXRot(realPitch);
		fakeRotation = false;
	}
	
	public void faceVectorPacket(Vec3 vec)
	{
		Rotation needed = RotationUtils.getNeededRotations(vec);
		LocalPlayer player = WurstClient.MC.player;
		
		fakeRotation = true;
		serverYaw =
			RotationUtils.limitAngleChange(player.getYRot(), needed.yaw());
		serverPitch = needed.pitch();
	}
	
	public void faceVectorClient(Vec3 vec)
	{
		Rotation needed = RotationUtils.getNeededRotations(vec);
		
		LocalPlayer player = WurstClient.MC.player;
		player.setYRot(
			RotationUtils.limitAngleChange(player.getYRot(), needed.yaw()));
		player.setXRot(needed.pitch());
	}
	
	public void faceVectorClientIgnorePitch(Vec3 vec)
	{
		Rotation needed = RotationUtils.getNeededRotations(vec);
		
		LocalPlayer player = WurstClient.MC.player;
		player.setYRot(
			RotationUtils.limitAngleChange(player.getYRot(), needed.yaw()));
		player.setXRot(0);
	}
	
	public float getServerYaw()
	{
		return fakeRotation ? serverYaw : WurstClient.MC.player.getYRot();
	}
	
	public float getServerPitch()
	{
		return fakeRotation ? serverPitch : WurstClient.MC.player.getXRot();
	}
}
