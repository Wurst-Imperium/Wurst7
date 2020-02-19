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
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
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
			"tesselateSmooth(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;ZLjava/util/Random;J)Z",
			"tesselateFlat(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;ZLjava/util/Random;J)Z"},
		cancellable = true)
	private void onTesselateSmoothOrFlat(BlockRenderView extendedBlockView_1,
		BakedModel bakedModel_1, BlockState blockState_1, BlockPos blockPos_1,
		BufferBuilder bufferBuilder_1, boolean depthTest, Random random_1,
		long long_1, CallbackInfoReturnable<Boolean> cir)
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
		
		tesselateSmooth(extendedBlockView_1, bakedModel_1, blockState_1,
			blockPos_1, bufferBuilder_1, false, random_1, long_1);
	}
	
	@Shadow
	public boolean tesselateSmooth(BlockRenderView extendedBlockView_1,
		BakedModel bakedModel_1, BlockState blockState_1, BlockPos blockPos_1,
		BufferBuilder bufferBuilder_1, boolean boolean_1, Random random_1,
		long long_1)
	{
		return false;
	}
}
