/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.item.ItemConvertible;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;
import net.wurstclient.hacks.XRayHack;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin implements ItemConvertible
{
	private static ThreadLocal<Float> currentOpacity =
		ThreadLocal.withInitial(() -> 1F);
	
	/**
	 * Makes X-Ray work when neither Sodium nor Indigo are running. Also gets
	 * called while Indigo is running when breaking a block in survival mode or
	 * seeing a piston retract.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/block/Block;shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z"),
		method = "shouldDrawFace(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;ZLnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;)Z")
	private static boolean onRenderSmoothOrFlat(BlockState state,
		BlockState otherState, Direction side, Operation<Boolean> original,
		BlockRenderView world, BlockState stateButFromTheOtherMethod,
		boolean cull, Direction sideButFromTheOtherMethod, BlockPos pos)
	{
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode() || xray.isVisible(state.getBlock(), pos))
			currentOpacity.set(1F);
		else
			currentOpacity.set(xray.getOpacityFloat());
		
		if(event.isRendered() != null)
			return event.isRendered();
		
		return original.call(state, otherState, side);
	}
	
	/**
	 * Applies X-Ray's opacity mask to the block color after all the normal
	 * coloring and shading is done, if neither Sodium nor Indigo are running.
	 */
	@ModifyConstant(
		method = "renderQuad(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;Lnet/minecraft/client/render/block/BlockModelRenderer$LightmapCache;I)V",
		constant = @Constant(floatValue = 1F))
	private float modifyOpacity(float original)
	{
		return currentOpacity.get();
	}
}
