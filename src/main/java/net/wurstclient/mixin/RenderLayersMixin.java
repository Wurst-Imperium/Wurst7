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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.FluidState;
import net.wurstclient.WurstClient;

@Mixin(RenderLayers.class)
public abstract class RenderLayersMixin
{
	/**
	 * Puts all blocks on the translucent layer if Opacity X-Ray is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getBlockLayer(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/BlockRenderLayer;",
		cancellable = true)
	private static void onGetBlockLayer(BlockState state,
		CallbackInfoReturnable<BlockRenderLayer> cir)
	{
		if(!WurstClient.INSTANCE.getHax().xRayHack.isOpacityMode())
			return;
		
		cir.setReturnValue(BlockRenderLayer.TRANSLUCENT);
	}
	
	/**
	 * Puts all fluids on the translucent layer if Opacity X-Ray is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getFluidLayer(Lnet/minecraft/fluid/FluidState;)Lnet/minecraft/client/render/BlockRenderLayer;",
		cancellable = true)
	private static void onGetFluidLayer(FluidState state,
		CallbackInfoReturnable<BlockRenderLayer> cir)
	{
		if(!WurstClient.INSTANCE.getHax().xRayHack.isOpacityMode())
			return;
		
		cir.setReturnValue(BlockRenderLayer.TRANSLUCENT);
	}
}
