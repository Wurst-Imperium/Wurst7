/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.WeightedBakedModel;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawFacelessModelListener.ShouldDrawFacelessModelEvent;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin {
    @Inject(at = @At("HEAD"), method = "getQuads", cancellable = true)
    private void getQuads(@Nullable BlockState state, @Nullable Direction face,
        Random random, CallbackInfoReturnable<List<BakedQuad>> cir)
    {
        if (face != null || state == null) return;

        ShouldDrawFacelessModelEvent event = new ShouldDrawFacelessModelEvent(state);
        EventManager.fire(event);

        if(event.isCancelled())
            cir.setReturnValue(List.of());
    }
}
