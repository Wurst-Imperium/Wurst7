/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer"},
	remap = false)
public class SodiumFluidRendererMixin
{
	@Unique
	private ThreadLocal<BlockPos.Mutable> mutablePosForExposedCheck =
		ThreadLocal.withInitial(BlockPos.Mutable::new);
	
	/**
	 * This mixin hides and shows fluids when using X-Ray with Sodium installed.
	 * Last updated for Sodium 0.6.0-beta.1+mc1.21.
	 */
	@Inject(at = @At("HEAD"), method = "isFluidOccluded", cancellable = true)
	private void onIsFluidOccluded(BlockRenderView world, int x, int y, int z,
		Direction dir, BlockState state, Fluid fluid,
		CallbackInfoReturnable<Boolean> cir)
	{
		BlockPos.Mutable pos = mutablePosForExposedCheck.get();
		pos.set(x, y, z);
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(!event.isRendered());
	}
}
