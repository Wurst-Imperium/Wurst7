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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
	@WrapOperation(
		method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
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
	@ModifyConstant(
		method = "putQuadData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;Lnet/minecraft/client/renderer/block/ModelBlockRenderer$CommonRenderStorage;I)V",
		constant = @Constant(floatValue = 1F))
	private float modifyOpacity(float original)
	{
		return currentOpacity.get();
	}
	
	/**
	 * Hides blocks like grass and snow when neither Sodium nor Indigo are
	 * running.
	 */
	@WrapOperation(method = {
		"tesselateWithoutAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V",
		"tesselateWithAO(Lnet/minecraft/world/level/BlockAndTintGetter;Ljava/util/List;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZI)V"},
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;isEmpty()Z",
			ordinal = 1))
	private boolean pretendEmptyToStopSecondRenderModelFaceFlatCall(
		List<BakedQuad> instance, Operation<Boolean> original,
		BlockAndTintGetter world, List<BlockModelPart> list, BlockState state,
		BlockPos pos, PoseStack poseStack, VertexConsumer vertexConsumer,
		boolean cull, int light)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		Boolean shouldDrawSide = xray.shouldDrawSide(state, pos);
		
		if(Boolean.FALSE.equals(shouldDrawSide))
			return true;
		
		return original.call(instance);
	}
}
