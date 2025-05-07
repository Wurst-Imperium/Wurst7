/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.wurstclient.WurstClient;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin
{
	/**
	 * Removes the distance fog when NoFog is enabled.
	 *
	 * As of 25w16a, this also seems to disable clouds for some reason.
	 */
	@ModifyExpressionValue(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/render/BackgroundRenderer;fogEnabled:Z"),
		method = "getFogBuffer(Lnet/minecraft/client/render/BackgroundRenderer$FogType;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")
	private boolean onGetFogBuffer(boolean original)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
			return original;
		
		MinecraftClient mc = WurstClient.MC;
		Camera camera = mc.gameRenderer.getCamera();
		CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
		if(cameraSubmersionType != CameraSubmersionType.NONE)
			return original;
			
		// Entity entity = camera.getFocusedEntity();
		// float tickProgress =
		// mc.getRenderTickCounter().getTickProgress(false);
		// if(BackgroundRenderer.getFogModifier(entity, tickProgress) != null)
		// return original;
		
		return false;
	}
	
	// TODO: Figure out how to do this in 1.21.6
	// @Inject(at = @At("HEAD"),
	// method =
	// "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;",
	// cancellable = true)
	// private static void onGetFogModifier(Entity entity, float tickDelta,
	// CallbackInfoReturnable<StatusEffectFogModifier> ci)
	// {
	// if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
	// ci.setReturnValue(null);
	// }
}
