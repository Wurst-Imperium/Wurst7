/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;

@Mixin(Camera.class)
public abstract class CameraMixin implements TrackedWaypoint.Camera
{
	@Shadow
	private boolean detached;
	
	@Inject(
		method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V",
		at = @At("RETURN"),
		cancellable = false)
	public void onSetup(Level level, Entity entity, boolean bl, boolean bl2,
		float partialTicks, CallbackInfo ci)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(!freecam.isEnabled())
			return;
		
		detached = true;
		setPosition(freecam.getCamPos(partialTicks));
		setRotation(freecam.getCamYaw(), freecam.getCamPitch());
	}
	
	@Shadow
	protected abstract void setPosition(Vec3 pos);
	
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);
}
