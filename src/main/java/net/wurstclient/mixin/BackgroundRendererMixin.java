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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.MobEffectFogFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.wurstclient.WurstClient;

@Mixin(FogRenderer.class)
public abstract class BackgroundRendererMixin
{
	/**
	 * Makes the distance fog 100% transparent when NoFog is enabled,
	 * effectively removing it.
	 */
	@Inject(at = @At("HEAD"),
		method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V")
	private static void onApplyFog(Camera camera, FogRenderer.FogMode fogType,
		float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled()
			|| fogType != FogRenderer.FogMode.FOG_TERRAIN)
			return;
		
		FogType cameraSubmersionType = camera.getFluidInCamera();
		if(cameraSubmersionType != FogType.NONE)
			return;
		
		Entity entity = camera.getEntity();
		if(FogRenderer.getPriorityFogFunction(entity, tickDelta) != null)
			return;
		
		RenderSystem.setShaderFogColor(0, 0, 0, 0);
	}
	
	@Inject(at = @At("HEAD"),
		method = "getPriorityFogFunction(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/FogRenderer$MobEffectFogFunction;",
		cancellable = true)
	private static void onGetFogModifier(Entity entity, float tickDelta,
		CallbackInfoReturnable<MobEffectFogFunction> ci)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			ci.setReturnValue(null);
	}
}
