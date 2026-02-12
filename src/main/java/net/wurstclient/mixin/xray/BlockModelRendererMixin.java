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
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.block.BakedQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.XRayHack;

@Mixin(ModelBlockRenderer.class)
public abstract class BlockModelRendererMixin implements ItemLike
{
	private static ThreadLocal<Float> currentOpacity =
		ThreadLocal.withInitial(() -> 1F);
	
	/**
	 * Makes X-Ray work when neither Sodium nor Indigo are running. Also gets
	 * called while Indigo is running when breaking a block in survival mode or
	 * seeing a piston retract.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"),
		method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z")
	private static boolean onRenderSmoothOrFlat(BlockState state,
		BlockState otherState, Direction side, Operation<Boolean> original,
		BlockAndTintGetter world, BlockState stateButFromTheOtherMethod,
		boolean cull, Direction sideButFromTheOtherMethod, BlockPos neighborPos)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		BlockPos pos = neighborPos.relative(side.getOpposite());
		Boolean shouldDrawSide = xray.shouldDrawSide(state, pos);
		
		if(!xray.isOpacityMode() || xray.isVisible(state.getBlock(), pos))
			currentOpacity.set(1F);
		else
			currentOpacity.set(xray.getOpacityFloat());
		
		if(shouldDrawSide != null)
			return shouldDrawSide;
		
		return original.call(state, otherState, side);
	}
	
	/**
	 * Applies X-Ray's opacity mask to the block color after all the normal
	 * coloring and shading is done, if neither Sodium nor Indigo are running.
	 */
	@ModifyArg(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/block/BakedQuadOutput;put(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadBrightness;ILcom/mojang/blaze3d/vertex/QuadLightmapCoords;I)V"),
		method = "putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/block/BakedQuadOutput;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/minecraft/client/renderer/block/ModelBlockRenderer$CommonRenderStorage;I)V",
		index = 3)
	private int modifyOpacity(int tintColor)
	{
		float opacity = currentOpacity.get();
		if(opacity >= 1F)
			return tintColor;
		
		return ARGB.color(Math.round(255F * opacity), ARGB.red(tintColor),
			ARGB.green(tintColor), ARGB.blue(tintColor));
	}
	
	/**
	 * Hides blocks like grass and snow when neither Sodium nor Indigo are
	 * running.
	 */
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;isEmpty()Z",
			ordinal = 1),
		method = {
			"tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/BakedQuadOutput;ZI)V",
			"tesselateWithAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/block/BakedQuadOutput;ZI)V"})
	private boolean pretendEmptyToStopSecondRenderModelFaceFlatCall(
		List<BakedQuad> instance, Operation<Boolean> original,
		BlockAndTintGetter world, List<BlockModelPart> list, BlockState state,
		BlockPos pos, PoseStack poseStack, BakedQuadOutput output, boolean cull,
		int light)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		Boolean shouldDrawSide = xray.shouldDrawSide(state, pos);
		
		if(Boolean.FALSE.equals(shouldDrawSide))
			return true;
		
		return original.call(instance);
	}
}
