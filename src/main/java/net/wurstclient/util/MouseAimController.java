/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.events.MouseUpdateListener.MouseUpdateEvent;

/**
 * Plans a rotation once per tick and applies it as mouse input between ticks.
 */
public final class MouseAimController
{
	private static final Minecraft MC = WurstClient.MC;
	
	private Rotation curTickRot;
	private Rotation nextTickRot;
	
	public void reset()
	{
		curTickRot = null;
		nextTickRot = null;
	}
	
	public void tick(Vec3 aimPoint, float degreesPerSecond)
	{
		curTickRot = nextTickRot != null ? nextTickRot
			: new Rotation(MC.player.getYRot(), MC.player.getXRot());
		
		nextTickRot = curTickRot.slowlyTurnTowards(
			RotationUtils.getNeededRotations(aimPoint), degreesPerSecond / 20F);
		
		// Vulcan D/F flag >1.825° yaw changes with unchanged pitch
		if(Math.abs(nextTickRot.yaw() - curTickRot.yaw()) > 1.825F
			&& nextTickRot.pitch() == curTickRot.pitch())
		{
			float pitchOffset = (float)(RotationUtils.getDegreesPerMouseUnit()
				* (Math.random() < 0.5 ? -1 : 1));
			nextTickRot = nextTickRot.add(0, pitchOffset).clampPitch();
		}
	}
	
	public void applyTo(MouseUpdateEvent event, double ignoreMouseInput)
	{
		if(MC.player == null || nextTickRot == null)
			return;
		
		Rotation curFrameRot =
			new Rotation(MC.player.getYRot(), MC.player.getXRot());
		
		float partialTicks =
			MC.getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Rotation nextFrameRot =
			Rotation.lerp(partialTicks, curTickRot, nextTickRot);
		
		double inputFactor = 1 - ignoreMouseInput;
		double inputX = Math.rint(event.getDefaultDeltaX() * inputFactor);
		double inputY = Math.rint(event.getDefaultDeltaY() * inputFactor);
		
		float yawDiff = nextFrameRot.yaw() - curFrameRot.yaw();
		float pitchDiff = nextFrameRot.pitch() - curFrameRot.pitch();
		double degPerMouseUnit = RotationUtils.getDegreesPerMouseUnit();
		double aimX = Math.rint(yawDiff / degPerMouseUnit);
		double aimY = Math.rint(pitchDiff / degPerMouseUnit);
		
		event.setDeltaX(inputX + aimX);
		event.setDeltaY(inputY + aimY);
		
		if(!MC.mouseHandler.isMouseGrabbed())
			return;
		
		// Carry manual movement into the aim path so later frames don't undo it
		float inputYaw = (float)(inputX * degPerMouseUnit);
		float inputPitch = (float)(inputY * degPerMouseUnit);
		curTickRot = curTickRot.add(inputYaw, inputPitch).clampPitch();
		nextTickRot = nextTickRot.add(inputYaw, inputPitch).clampPitch();
	}
}
