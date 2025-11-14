/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.FogShape;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogParameters;
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
	@WrapOperation(
		at = @At(value = "NEW",
			target = "net/minecraft/client/renderer/FogParameters"),
		method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;Lorg/joml/Vector4f;FZF)Lnet/minecraft/client/renderer/FogParameters;")
	private static FogParameters createTransparentFog(float start, float end,
		FogShape shape, float red, float green, float blue, float alpha,
		Operation<FogParameters> original, Camera camera,
		FogRenderer.FogMode fogType, Vector4f color, float viewDistance,
		boolean thickenFog, float tickDelta)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled()
			|| fogType != FogRenderer.FogMode.FOG_TERRAIN)
			return original.call(start, end, shape, red, green, blue, alpha);
		
		FogType cameraSubmersionType = camera.getFluidInCamera();
		if(cameraSubmersionType != FogType.NONE)
			return original.call(start, end, shape, red, green, blue, alpha);
		
		Entity entity = camera.getEntity();
		if(FogRenderer.getPriorityFogFunction(entity, tickDelta) != null)
			return original.call(start, end, shape, red, green, blue, alpha);
		
		return original.call(start, end, shape, 0F, 0F, 0F, 0F);
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
