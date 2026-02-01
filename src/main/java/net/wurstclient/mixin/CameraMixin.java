/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.CameraDistanceHack;
import net.wurstclient.hacks.FreecamHack;

@Mixin(Camera.class)
public abstract class CameraMixin
{
	@Shadow
	private Vec3 position;
	
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);
	
	@ModifyVariable(at = @At("HEAD"),
		method = "getMaxZoom(F)F",
		argsOnly = true)
	private float changeClipToSpaceDistance(float desiredCameraDistance)
	{
		CameraDistanceHack cameraDistance =
			WurstClient.INSTANCE.getHax().cameraDistanceHack;
		if(cameraDistance.isEnabled())
			return cameraDistance.getDistance();
		
		return desiredCameraDistance;
	}
	
	@Inject(at = @At("HEAD"), method = "getMaxZoom(F)F", cancellable = true)
	private void onClipToSpace(float desiredCameraDistance,
		CallbackInfoReturnable<Float> cir)
	{
		if(WurstClient.INSTANCE.getHax().cameraNoClipHack.isEnabled())
			cir.setReturnValue(desiredCameraDistance);
	}
	
	@Inject(at = @At("HEAD"),
		method = "getFluidInCamera()Lnet/minecraft/world/level/material/FogType;",
		cancellable = true)
	private void onGetSubmersionType(CallbackInfoReturnable<FogType> cir)
	{
		if(WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled())
			cir.setReturnValue(FogType.NONE);
	}
	
	@Inject(at = @At("TAIL"),
		method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V")
	private void onSetupCamera(Level level, Entity focusedEntity,
		boolean thirdPerson, boolean inverseView, float tickDelta,
		CallbackInfo ci)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(!freecam.isAiCompatibilityMode())
			return;
		
		position = new Vec3(freecam.getCamX(tickDelta),
			freecam.getCamY(tickDelta), freecam.getCamZ(tickDelta));
		setRotation(freecam.getCamYaw(), freecam.getCamPitch());
	}
	
	@Inject(at = @At("HEAD"), method = "isDetached()Z", cancellable = true)
	private void onIsDetached(CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().freecamHack.isAiCompatibilityMode())
			cir.setReturnValue(true);
	}
}
