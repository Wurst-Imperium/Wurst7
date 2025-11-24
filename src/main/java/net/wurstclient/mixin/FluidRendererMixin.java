/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;
import net.wurstclient.hacks.XRayHack;

@Mixin(LiquidBlockRenderer.class)
public class FluidRendererMixin
{
	@Unique
	private static final ThreadLocal<Float> currentOpacity =
		ThreadLocal.withInitial(() -> 1F);
	
	/**
	 * Hides and shows fluids when using X-Ray without Sodium installed.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"),
		method = "tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V")
	private boolean modifyShouldSkipRendering(Direction side, float height,
		BlockState neighborState, Operation<Boolean> original,
		BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer,
		BlockState blockState, FluidState fluidState)
	{
		// Note: the null BlockPos is here to skip the "exposed only" check
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(blockState, null);
		EventManager.fire(event);
		
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode() || xray.isVisible(blockState.getBlock(), pos))
			currentOpacity.set(1F);
		else
			currentOpacity.set(xray.getOpacityFloat());
		
		if(event.isRendered() != null)
			return !event.isRendered();
		
		return original.call(side, height, neighborState);
	}
	
	/**
	 * Modifies opacity of fluids when using X-Ray without Sodium installed.
	 */
	@ModifyConstant(
		method = "vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFFFFFFI)V",
		constant = @Constant(floatValue = 1F, ordinal = 0))
	private float modifyOpacity(float original)
	{
		return currentOpacity.get();
	}
}
