/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.RenderListener.RenderEvent;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
	@Inject(
		method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
		at = @At("RETURN"))
	private void onRender(GraphicsResourceAllocator allocator,
		DeltaTracker tickCounter, boolean renderBlockOutline,
		CameraRenderState cameraState, Matrix4fc positionMatrix,
		GpuBufferSlice gpuBufferSlice, Vector4f vector4f,
		boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender,
		CallbackInfo ci)
	{
		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(positionMatrix);
		float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);
		RenderEvent event = new RenderEvent(matrixStack, tickProgress);
		EventManager.fire(event);
	}
}
