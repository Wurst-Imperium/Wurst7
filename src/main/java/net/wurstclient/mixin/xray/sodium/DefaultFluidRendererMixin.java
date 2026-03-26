/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.XRayHack;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/blob/148316fbfca6c3c88274ad79e1010310c6a3749b/common/src/main/java/net/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer.java">Sodium
 * mc26.1-0.8.7</a>
 */
@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"})
public class DefaultFluidRendererMixin
{
	/**
	 * This, together with {@code onIsFluidSideExposed(...)}, hides and shows
	 * fluid side faces when using X-Ray with Sodium.
	 */
	@Inject(
		method = "isFullBlockFluidSideVisible(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z",
		at = @At("HEAD"),
		cancellable = true,
		remap = false,
		require = 0)
	private void onIsFullBlockFluidSideVisible(BlockGetter world, BlockPos pos,
		Direction dir, FluidState fluid, CallbackInfoReturnable<Boolean> cir)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		BlockState state = world.getBlockState(pos);
		
		// Note: the null BlockPos is here to skip the "exposed only" check
		Boolean shouldDrawSide = xray.shouldDrawSide(state, null);
		
		if(shouldDrawSide == null)
			return;
		
		BlockPos nPos = pos.offset(dir.getUnitVec3i());
		BlockState neighborState = world.getBlockState(nPos);
		
		cir.setReturnValue(
			!neighborState.getFluidState().getType().isSame(fluid.getType())
				&& shouldDrawSide);
	}
	
	/**
	 * This, together with {@code onIsFullBlockFluidSideVisible(...)}, hides and
	 * shows fluid side faces when using X-Ray with Sodium.
	 *
	 * It also slightly breaks the shape of flowing water when X-Ray is enabled
	 * because of the way Sodium's {@code fluidCornerHeight(...)} works. This is
	 * annoying to work around so I left it as-is.
	 */
	@Inject(
		method = "isFluidSideExposed(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Z",
		at = @At("HEAD"),
		cancellable = true,
		remap = false,
		require = 0)
	private void onIsFluidSideExposed(BlockAndTintGetter world,
		BlockState state, BlockPos neighborPos, Direction dir, float height,
		CallbackInfoReturnable<Boolean> cir)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		
		// Note: the null BlockPos is here to skip the "exposed only" check
		Boolean shouldDrawSide = xray.shouldDrawSide(state, null);
		
		if(shouldDrawSide == null)
			return;
		
		BlockState neighborState = world.getBlockState(neighborPos);
		cir.setReturnValue(!neighborState.getFluidState().getType()
			.isSame(state.getFluidState().getType()) && shouldDrawSide);
	}
	
	/**
	 * Overrides Sodium's hidden-fluid-culling flood fill for source/full
	 * fluids. Returning 3 marks the upward face as exposed from both
	 * directions, which keeps underground top faces visible for X-Ray.
	 */
	@Inject(
		method = "getUpFaceExposureByNeighbors(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/FluidState;)I",
		at = @At("HEAD"),
		cancellable = true,
		remap = false,
		require = 0)
	private void onGetUpFaceExposureByNeighbors(BlockAndTintGetter level,
		BlockPos pos, FluidState fluidState,
		CallbackInfoReturnable<Integer> cir)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		Boolean shouldDrawSide =
			xray.shouldDrawSide(fluidState.createLegacyBlock(), null);
		
		if(shouldDrawSide != null)
			cir.setReturnValue(shouldDrawSide ? 3 : 0);
	}
	
	/**
	 * Modifies opacity of fluids when using X-Ray with Sodium installed.
	 */
	@ModifyExpressionValue(method = "updateQuad",
		at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I"),
		remap = false,
		require = 0)
	private int onUpdateQuad(int original, @Local(argsOnly = true) BlockPos pos,
		@Local(argsOnly = true) FluidState state)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode()
			|| xray.isVisible(state.createLegacyBlock().getBlock(), pos))
			return original;
		
		return original & xray.getOpacityColorMask();
	}
}
