/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.RenderListener.RenderEvent;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin
{
	@Inject(at = @At("HEAD"),
		method = "doesMobEffectBlockSky(Lnet/minecraft/client/Camera;)Z",
		cancellable = true)
	private void onHasBlindnessOrDarkness(Camera camera,
		CallbackInfoReturnable<Boolean> ci)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			ci.setReturnValue(false);
	}
	
	@Inject(at = @At("RETURN"),
		method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V")
	private void onRender(GraphicsResourceAllocator allocator,
		DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera,
		Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f matrix4f2,
		GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl,
		CallbackInfo ci)
	{
		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(positionMatrix);
		float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);
		RenderEvent event = new RenderEvent(matrixStack, tickProgress);
		EventManager.fire(event);
	}
}
