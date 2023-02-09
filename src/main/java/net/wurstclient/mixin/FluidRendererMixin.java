/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

@Mixin(FluidRenderer.class)
public class FluidRendererMixin
{
	@Inject(at = {@At("HEAD")},
		method = {
			"isSideCovered(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;FLnet/minecraft/block/BlockState;)Z"},
		cancellable = true)
	private static void onIsSideCovered(BlockView blockView, BlockPos blockPos,
		Direction direction, float maxDeviation, BlockState blockState,
		CallbackInfoReturnable<Boolean> cir)
	{
		BlockState state = blockView.getBlockState(blockPos);
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
}
