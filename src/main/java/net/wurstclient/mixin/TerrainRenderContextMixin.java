/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.WurstClient;
import net.wurstclient.events.TesselateBlockListener.TesselateBlockEvent;

@Mixin(TerrainRenderContext.class)
public class TerrainRenderContextMixin
{
	@Inject(at = {@At("HEAD")},
		method = {"tesselateBlock"},
		cancellable = true,
		remap = false)
	private void tesselateBlock(BlockState blockState, BlockPos blockPos,
		final BakedModel model, MatrixStack matrixStack,
		CallbackInfoReturnable<Boolean> cir)
	{
		TesselateBlockEvent event = new TesselateBlockEvent(blockState);
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			cir.cancel();
	}
}
