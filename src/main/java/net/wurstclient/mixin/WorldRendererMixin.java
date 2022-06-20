/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.wurstclient.WurstClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Inject(at = {@At("HEAD")},
			method = {
					"method_43788(Lnet/minecraft/client/render/Camera;)Z"},
			cancellable = true)
	private void method_43788(Camera camera, CallbackInfoReturnable<Boolean> ci) {
		if (WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			ci.setReturnValue(false);
	}
}

