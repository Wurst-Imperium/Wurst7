/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.zoom;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.GameRenderer;
import net.wurstclient.WurstClient;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	/**
	 * Applies zoom to both the world and first-person hand FOV.
	 */
	@ModifyReturnValue(method = "getFov(Lnet/minecraft/client/Camera;FZ)F",
		at = @At("RETURN"))
	private float onGetFov(float original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
}
