/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.XRayHack;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin
{
	@Unique
	private static final ThreadLocal<Float> currentOpacity =
		ThreadLocal.withInitial(() -> 1F);
	
	/**
	 * Hides and shows fluids when using X-Ray without Sodium installed.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"),
		method = "tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V")
	private boolean modifyShouldSkipRendering(Direction side, float height,
		BlockState neighborState, Operation<Boolean> original,
		BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer,
		BlockState blockState, FluidState fluidState)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		
		// Note: the null BlockPos is here to skip the "exposed only" check
		Boolean shouldDrawSide = xray.shouldDrawSide(blockState, null);
		
		if(!xray.isOpacityMode() || xray.isVisible(blockState.getBlock(), pos))
			currentOpacity.set(1F);
		else
			currentOpacity.set(xray.getOpacityFloat());
		
		if(shouldDrawSide != null)
			return !shouldDrawSide;
		
		return original.call(side, height, neighborState);
	}
	
	/**
	 * Modifies opacity of fluids when using X-Ray without Sodium installed.
	 */
	@ModifyArg(at = @At(value = "INVOKE",
		target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"),
		method = "vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFIFFI)V",
		index = 3)
	private int modifyOpacity(int color)
	{
		float opacity = currentOpacity.get();
		if(opacity >= 1F)
			return color;
		
		return ARGB.color(Math.round(255F * opacity), ARGB.red(color),
			ARGB.green(color), ARGB.blue(color));
	}
	
	/**
	 * Puts all fluids on the translucent layer if Opacity X-Ray is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getRenderLayer(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;",
		cancellable = true)
	private void onGetFluidLayer(FluidState state,
		CallbackInfoReturnable<ChunkSectionLayer> cir)
	{
		if(!WurstClient.INSTANCE.getHax().xRayHack.isOpacityMode())
			return;
		
		cir.setReturnValue(ChunkSectionLayer.TRANSLUCENT);
	}
}
