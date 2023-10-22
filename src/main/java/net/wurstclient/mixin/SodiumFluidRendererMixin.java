/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidRenderer.class, remap = false)
public class SodiumFluidRendererMixin
{
    @Inject(at = @At("HEAD"),
        method = "isSideExposed",
        cancellable = true)
    private void isSideExposed(BlockRenderView world,
		int x, int y, int z, Direction dir, float height,
		CallbackInfoReturnable<Boolean> cir)
    {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, pos);
        EventManager.fire(event);

        if(event.isRendered() != null)
            cir.setReturnValue(event.isRendered());
    }
}
