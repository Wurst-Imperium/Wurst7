/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.nio.ByteBuffer;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.render.fog.FogRenderer;
import net.wurstclient.WurstClient;

@Mixin(FogRenderer.class)
public class FogRendererMixin
{
	/**
	 * Removes the fog near the end of the render distance in all dimensions, if
	 * NoFog is enabled.
	 */
	@WrapOperation(
		method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
	private void wrapApplyFog(FogRenderer instance, ByteBuffer buffer,
		int bufPos, Vector4f fogColor, float environmentalStart,
		float environmentalEnd, float renderDistanceStart,
		float renderDistanceEnd, float skyEnd, float cloudEnd,
		Operation<Void> original)
	{
		if(WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
		{
			renderDistanceStart = 1000000;
			renderDistanceEnd = 1000000;
		}
		
		original.call(instance, buffer, bufPos, fogColor, environmentalStart,
			environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd,
			cloudEnd);
	}
}
