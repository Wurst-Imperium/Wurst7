/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.wurstclient.WurstClient;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin
{
	/**
	 * @author IUDevman
	 *
	 *         Removes the circular fog that appears around the player near the
	 *         end of the render distance.
	 *		
	 *         NOTE: This mixin injects right before the mappedView try loop.
	 *         Modifying these render variables at any other time point does not
	 *         work.
	 */
	@SuppressWarnings("u")
	@Inject(
		method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;getDevice()Lcom/mojang/blaze3d/systems/GpuDevice;",
			remap = false))
	private void onApplyFog(Camera camera, int viewDistance, boolean thick,
		RenderTickCounter tickCounter, float skyDarkness, ClientWorld world,
		CallbackInfoReturnable<Vector4f> cir, @Local FogData fogData)
	{
		if(WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
		{
			fogData.renderDistanceStart *= 100;
			fogData.renderDistanceEnd *= 100;
		}
	}
}
