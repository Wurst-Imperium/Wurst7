/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.wurstclient.WurstClient;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin
{
	@Inject(at = {@At("HEAD")},
		method = {"renderFireOverlay()V"},
		cancellable = true)
	private static void onRenderFireOverlay(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noFireOverlayHack.isEnabled())
			ci.cancel();
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"renderWaterOverlay(F)V"},
		cancellable = true)
	private static void onRenderUnderwaterOverlay(float f, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled())
			ci.cancel();
	}
}
