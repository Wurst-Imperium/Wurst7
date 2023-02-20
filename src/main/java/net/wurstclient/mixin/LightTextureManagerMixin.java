/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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

import net.minecraft.client.render.LightmapTextureManager;
import net.wurstclient.WurstClient;

@Mixin(LightmapTextureManager.class)
public class LightTextureManagerMixin
{
	@Inject(at = {@At("HEAD")},
		method = {"getDarknessFactor(F)F"},
		cancellable = true)
	private void onGetDarknessFactor(float delta,
		CallbackInfoReturnable<Float> ci)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			ci.setReturnValue(0F);
	}
}
