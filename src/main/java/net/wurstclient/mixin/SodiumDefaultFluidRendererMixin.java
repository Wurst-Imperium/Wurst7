/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;
import net.wurstclient.hacks.XRayHack;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/tree/02253db283e4679228ba5fbc30cfc851d17123c8">Sodium
 * 0.6.13+mc1.21.6</a>
 */
@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"})
public class SodiumDefaultFluidRendererMixin
{
	/**
	 * Hides and shows fluids when using X-Ray with Sodium installed.
	 */
	@Inject(at = @At("HEAD"),
		method = "isFullBlockFluidOccluded(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z",
		cancellable = true,
		require = 0)
	private void onIsFullBlockFluidOccluded(BlockRenderView world, BlockPos pos,
		Direction dir, BlockState state, FluidState fluid,
		CallbackInfoReturnable<Boolean> cir)
	{
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
	
	/**
	 * Modifies opacity of fluids when using X-Ray with Sodium installed.
	 */
	@ModifyExpressionValue(at = @At(value = "INVOKE",
		target = "Lnet/caffeinemc/mods/sodium/api/util/ColorARGB;toABGR(I)I"),
		method = "updateQuad",
		require = 0,
		remap = false)
	private int onUpdateQuad(int original, @Local(argsOnly = true) BlockPos pos,
		@Local(argsOnly = true) FluidState state)
	{
		XRayHack xray = WurstClient.INSTANCE.getHax().xRayHack;
		if(!xray.isOpacityMode()
			|| xray.isVisible(state.getBlockState().getBlock(), pos))
			return original;
		
		return original & xray.getOpacityColorMask();
	}
}
