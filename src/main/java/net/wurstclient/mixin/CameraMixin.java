/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.level.material.FogType;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.CameraDistanceHack;

@Mixin(Camera.class)
public abstract class CameraMixin
{
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
	
	/**
	 * Prevents blindness and darkness effects from changing the sky when
	 * AntiBlind is enabled.
	 *
	 * <p>
	 * In 26.1-snapshot-7, those effects don't appear to visibly change the sky
	 * even without this mixin. Might be a bug in that snapshot.
	 */
	@Inject(at = @At("RETURN"),
		method = "extractRenderState(Lnet/minecraft/client/renderer/state/CameraRenderState;F)V")
	private void onExtractRenderState(CameraRenderState cameraState,
		float partialTicks, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			cameraState.entityRenderState.doesMobEffectBlockSky = false;
	}
	
	/**
	 * Makes the zoom work.
	 */
	@ModifyReturnValue(at = @At("RETURN"), method = "calculateFov(F)F")
	private float onCalculateFov(float original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
	
	/**
	 * Moves the hand in first person mode out of the way as you zoom in
	 * further.
	 */
	@ModifyReturnValue(at = @At("RETURN"), method = "calculateHudFov(F)F")
	private float onCalculateHudFov(float original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
}
