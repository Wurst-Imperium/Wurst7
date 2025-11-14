/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

@Pseudo
@Mixin(targets = {
	"net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache"},
	remap = false)
public class BlockOcclusionCacheMixin
{
	/**
	 * This mixin hides and shows regular full blocks when using X-Ray with
	 * Sodium installed. Last updated for Sodium 0.6.0-beta.1+mc1.21.
	 *
	 * <p>
	 * Works with Sodium >=0.6.0-beta.1 and <0.6.13.
	 */
	@Inject(at = @At("HEAD"), method = "shouldDrawSide", cancellable = true)
	public void shouldDrawSide(BlockState state, BlockGetter world,
		BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir)
	{
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(event.isRendered());
	}
}
