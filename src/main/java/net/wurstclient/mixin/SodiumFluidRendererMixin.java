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

@Pseudo
@Mixin(targets = {
	// current target
	"me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer",
	// < Sodium 0.4.9
	"me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer"},
	remap = false)
public class SodiumFluidRendererMixin
{
	// Until Sodium updates to 1.21.2, there is no way to tell what this mixin
	// needs to look like.
	
	// /**
	// * This mixin hides and shows fluids when using X-Ray with Sodium
	// installed.
	// */
	// @Inject(at = @At("HEAD"), method = "isSideExposed", cancellable = true)
	// private void isSideExposed(BlockRenderView world, int x, int y, int z,
	// Direction dir, float height, CallbackInfoReturnable<Boolean> cir)
	// {
	// BlockPos pos = new BlockPos(x, y, z);
	// BlockState state = world.getBlockState(pos);
	// ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
	// EventManager.fire(event);
	//
	// if(event.isRendered() != null)
	// cir.setReturnValue(event.isRendered());
	// }
}
