/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener.ShouldDrawSideEvent;

@Mixin(SimpleBakedModel.class)
public class BasicBakedModelMixin
{
	/**
	 * This mixin hides blocks like grass and snow when using X-Ray. It works
	 * with and without Sodium installed.
	 */
	@Inject(at = @At("HEAD"), method = "getQuads", cancellable = true)
	private void getQuads(@Nullable BlockState state, @Nullable Direction face,
		RandomSource random, CallbackInfoReturnable<List<BakedQuad>> cir)
	{
		if(face != null || state == null
			|| !WurstClient.INSTANCE.getHax().xRayHack.isEnabled())
			return;
		
		ShouldDrawSideEvent event = new ShouldDrawSideEvent(state, null);
		EventManager.fire(event);
		
		if(Boolean.FALSE.equals(event.isRendered()))
			cir.setReturnValue(List.of());
	}
}
