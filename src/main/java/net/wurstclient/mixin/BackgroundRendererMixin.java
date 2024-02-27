/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.StatusEffectFogModifier;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.wurstclient.WurstClient;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin
{
	/**
	 * Makes the distance fog 100% transparent when NoFog is enabled,
	 * effectively removing it.
	 */
	@Inject(at = @At("HEAD"),
		method = "applyFog(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/BackgroundRenderer$FogType;FZF)V")
	private static void onApplyFog(Camera camera,
		BackgroundRenderer.FogType fogType, float viewDistance,
		boolean thickFog, float tickDelta, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
			return;
		
		CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
		if(cameraSubmersionType != CameraSubmersionType.NONE)
			return;
		
		Entity entity = camera.getFocusedEntity();
		if(BackgroundRenderer.getFogModifier(entity, tickDelta) != null)
			return;
		
		RenderSystem.setShaderFogColor(0, 0, 0, 0);
	}
	
	@Inject(at = @At("HEAD"),
		method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;",
		cancellable = true)
	private static void onGetFogModifier(Entity entity, float tickDelta,
		CallbackInfoReturnable<StatusEffectFogModifier> ci)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			ci.setReturnValue(null);
	}
}
