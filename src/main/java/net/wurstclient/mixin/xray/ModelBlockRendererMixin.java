/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.XRayHack;

@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin implements ItemLike
{
	/**
	 * Makes X-Ray work when neither Sodium nor Indigo are running. Also gets
	 * called while Indigo is running when breaking a block in survival mode or
	 * seeing a piston retract.
	 */
	@WrapOperation(
		method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
	private boolean onShouldRenderFace(BlockState state, BlockState otherState,
		Direction side, Operation<Boolean> original, BlockAndTintGetter world,
		BlockState stateButFromTheOtherMethod,
		Direction sideButFromTheOtherMethod, BlockPos neighborPos)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		BlockPos pos = neighborPos.relative(side.getOpposite());
		Boolean shouldDrawSide = xray.shouldDrawSide(state, pos);
		
		if(shouldDrawSide != null)
			return shouldDrawSide;
		
		return original.call(state, otherState, side);
	}
	
	/**
	 * Applies X-Ray's opacity mask to the block color after all the normal
	 * coloring and shading is done, if neither Sodium nor Indigo are running.
	 */
	@ModifyArg(
		method = "putQuadWithTint(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/resources/model/geometry/BakedQuad;)V",
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"))
	private int modifyOpacity(int color,
		@Local(argsOnly = true) BlockState state,
		@Local(argsOnly = true) BlockPos pos)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(xray.isOpacityMode() && !xray.isVisible(state.getBlock(), pos))
			return ARGB.color(
				Math.round(ARGB.alpha(color) * xray.getOpacityFloat()), color);
		
		return color;
	}
	
	/**
	 * Hides blocks like grass and snow when neither Sodium nor Indigo are
	 * running. Wraps the second {@code getQuads} call (the one with
	 * {@code null} direction for unculled quads) and returns an empty list
	 * when X-Ray should hide the block.
	 */
	@WrapOperation(method = {
		"tesselateFlat(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLjava/util/List;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
		"tesselateAmbientOcclusion(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLjava/util/List;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"},
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModelPart;getQuads(Lnet/minecraft/core/Direction;)Ljava/util/List;",
			ordinal = 1))
	private List<BakedQuad> hideUnculledQuads(BlockStateModelPart part,
		Direction direction, Operation<List<BakedQuad>> original,
		BlockQuadOutput output, float x, float y, float z,
		List<BlockStateModelPart> list, BlockAndTintGetter level,
		BlockState state, BlockPos pos)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		Boolean shouldDrawSide = xray.shouldDrawSide(state, pos);
		
		if(Boolean.FALSE.equals(shouldDrawSide))
			return List.of();
		
		return original.call(part, direction);
	}
}
