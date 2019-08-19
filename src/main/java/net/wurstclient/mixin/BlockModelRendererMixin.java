/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;
import net.wurstclient.WurstClient;
import net.wurstclient.events.RenderBlockModelListener.RenderBlockModelEvent;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin
{
	@Inject(at = {@At("HEAD")},
		method = {
			"tesselateSmooth(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;ZLjava/util/Random;J)Z"},
		cancellable = true)
	private void onTesselateSmooth(ExtendedBlockView extendedBlockView_1,
		BakedModel bakedModel_1, BlockState state, BlockPos blockPos_1,
		BufferBuilder bufferBuilder_1, boolean boolean_1, Random random_1,
		long long_1, CallbackInfoReturnable<Boolean> ci)
	{
		RenderBlockModelEvent event = new RenderBlockModelEvent(state);
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"tesselateFlat(Lnet/minecraft/world/ExtendedBlockView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/render/BufferBuilder;ZLjava/util/Random;J)Z"},
		cancellable = true)
	private void onTesselateFlat(ExtendedBlockView extendedBlockView_1,
		BakedModel bakedModel_1, BlockState state, BlockPos blockPos_1,
		BufferBuilder bufferBuilder_1, boolean boolean_1, Random random_1,
		long long_1, CallbackInfoReturnable<Boolean> ci)
	{
		RenderBlockModelEvent event = new RenderBlockModelEvent(state);
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
}
