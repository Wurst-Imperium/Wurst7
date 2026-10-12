/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.notelemetry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.wurstclient.WurstClient;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin
{
	/**
	 * Makes the telemetry button show its disabled state and tooltip if
	 * NoTelemetry is enabled.
	 */
	@ModifyExpressionValue(method = "init()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;allowsTelemetry()Z"))
	private boolean onAllowsTelemetryForButton(boolean original)
	{
		return original
			&& !WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled();
	}
}
