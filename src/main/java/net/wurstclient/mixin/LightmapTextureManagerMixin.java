/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.renderer.LightTexture;
import net.wurstclient.WurstClient;

@Mixin(LightTexture.class)
public class LightmapTextureManagerMixin
{
	/**
	 * Stops the other darkness effect in caves when AntiBlind is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getDarknessGamma(F)F",
		cancellable = true)
	private void onGetDarknessFactor(float delta,
		CallbackInfoReturnable<Float> cir)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			cir.setReturnValue(0F);
	}
}
