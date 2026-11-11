/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.XRayHack;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin
{
	/**
	 * Hides and shows fluids when using X-Ray without Sodium installed.
	 */
	@WrapOperation(
		method = "tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/FluidRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean modifyIsFaceOccludedByNeighbor(Direction side, float height,
		BlockState neighborState, Operation<Boolean> original,
		BlockAndTintGetter world, BlockPos pos, FluidRenderer.Output output,
		BlockState blockState, FluidState fluidState)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		
		// Note: the null BlockPos is here to skip the "exposed only" check
		Boolean shouldDrawSide = xray.shouldDrawSide(blockState, null);
		
		if(shouldDrawSide != null)
			return !shouldDrawSide;
		
		return original.call(side, height, neighborState);
	}
	
	/**
	 * Modifies opacity of fluids when using X-Ray without Sodium installed.
	 * Wraps the output after Fabric's handler dispatch, so recursive default
	 * rendering doesn't apply opacity twice.
	 */
	@ModifyVariable(
		method = "tesselate(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/FluidRenderer$Output;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;get(Lnet/minecraft/world/level/material/FluidState;)Lnet/minecraft/client/renderer/block/FluidModel;"),
		argsOnly = true)
	private FluidRenderer.Output modifyOpacity(FluidRenderer.Output original,
		@Local(argsOnly = true) BlockState state,
		@Local(argsOnly = true) BlockPos pos)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode() || xray.isVisible(state.getBlock(), pos))
			return original;
		
		float opacity = xray.getOpacityFloat();
		return (layer, x, y, z, quad, color, brightness,
			lightCoords) -> original.put(layer, x, y, z, quad,
				ARGB.color(Math.round(ARGB.alpha(color) * opacity), color),
				brightness, lightCoords);
	}
}
