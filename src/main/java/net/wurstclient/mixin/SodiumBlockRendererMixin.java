/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BlockRenderer.class, remap = false)
public class SodiumBlockRendererMixin
{
    @Inject(at = @At("HEAD"),
        method = "getGeometry",
        cancellable = true)
    private void getGeometry(BlockRenderContext ctx, Direction face,
		CallbackInfoReturnable<List<BakedQuad>> cir)
    {
        if (face != null) return;

        ShouldDrawSideEvent event = new ShouldDrawSideEvent(ctx.state(), ctx.pos());
        EventManager.fire(event);

        if (event.isRendered() != null && !event.isRendered())
            cir.setReturnValue(List.of());
    }
}
