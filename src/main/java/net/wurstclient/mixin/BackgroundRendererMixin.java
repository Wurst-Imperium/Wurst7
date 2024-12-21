/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.StatusEffectFogModifier;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import net.wurstclient.WurstClient;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin
{
	/**
	 * Makes the distance fog 100% transparent when NoFog is enabled,
	 * effectively removing it.
	 */
	@WrapOperation(
		at = @At(value = "NEW", target = "net/minecraft/client/render/Fog"),
		method = "applyFog(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/BackgroundRenderer$FogType;Lorg/joml/Vector4f;FZF)Lnet/minecraft/client/render/Fog;")
	private static Fog createTransparentFog(float start, float end,
		FogShape shape, float red, float green, float blue, float alpha,
		Operation<Fog> original, Camera camera,
		BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance,
		boolean thickenFog, float tickDelta)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
			return original.call(start, end, shape, red, green, blue, alpha);
		
		CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
		if(cameraSubmersionType != CameraSubmersionType.NONE)
			return original.call(start, end, shape, red, green, blue, alpha);
		
		Entity entity = camera.getFocusedEntity();
		if(BackgroundRenderer.getFogModifier(entity, tickDelta) != null)
			return original.call(start, end, shape, red, green, blue, alpha);
		
		return original.call(start, end, shape, 0F, 0F, 0F, 0F);
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
