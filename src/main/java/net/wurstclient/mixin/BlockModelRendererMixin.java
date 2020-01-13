/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.wurstclient.WurstClient;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;
import net.wurstclient.events.TesselateBlockListener.TesselateBlockEvent;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin
{
	@Inject(at = {@At("HEAD")},
		method = {
			"renderSmooth(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLjava/util/Random;JI)Z",
			"renderFlat(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLjava/util/Random;JI)Z"},
		cancellable = true)
	private void onRenderSmoothOrFlat(BlockRenderView blockRenderView_1,
		BakedModel bakedModel_1, BlockState blockState_1, BlockPos blockPos_1,
		MatrixStack matrixStack_1, VertexConsumer vertexConsumer_1,
		boolean depthTest, Random random_1, long long_1, int int_1,
		CallbackInfoReturnable<Boolean> cir)
	{
		TesselateBlockEvent event = new TesselateBlockEvent(blockState_1);
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
		{
			cir.cancel();
			return;
		}
		
		if(!depthTest)
			return;
		
		ShouldDrawSideEvent event2 = new ShouldDrawSideEvent(blockState_1);
		WurstClient.INSTANCE.getEventManager().fire(event2);
		if(!Boolean.TRUE.equals(event2.isRendered()))
			return;
		
		renderSmooth(blockRenderView_1, bakedModel_1, blockState_1, blockPos_1,
			matrixStack_1, vertexConsumer_1, false, random_1, long_1, int_1);
	}
	
	@Shadow
	public boolean renderSmooth(BlockRenderView blockRenderView_1,
		BakedModel bakedModel_1, BlockState blockState_1, BlockPos blockPos_1,
		MatrixStack matrixStack_1, VertexConsumer vertexConsumer_1,
		boolean boolean_1, Random random_1, long long_1, int int_1)
	{
		return false;
	}
}
