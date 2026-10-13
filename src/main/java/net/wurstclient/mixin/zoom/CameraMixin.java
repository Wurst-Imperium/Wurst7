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

import net.minecraft.client.Camera;
import net.wurstclient.WurstClient;

@Mixin(Camera.class)
public abstract class CameraMixin
{
	/**
	 * Makes the zoom work.
	 */
	@ModifyReturnValue(method = "calculateFov(F)F", at = @At("RETURN"))
	private float onCalculateFov(float original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
	
	/**
	 * Moves the hand in first person mode out of the way as you zoom in
	 * further.
	 */
	@ModifyReturnValue(method = "calculateHudFov(F)F", at = @At("RETURN"))
	private float onCalculateHudFov(float original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
}
