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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.MouseHandler;
import net.wurstclient.WurstClient;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin
{
	/**
	 * Reduces mouse sensitivity as you zoom in further.
	 */
	@ModifyExpressionValue(method = "turnPlayer(D)V",
		at = @At(value = "INVOKE", target = "Ljava/lang/Double;doubleValue()D"))
	private double onGetSensitivity(double original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeMouseSensitivityBasedOnZoom(original);
	}
}
