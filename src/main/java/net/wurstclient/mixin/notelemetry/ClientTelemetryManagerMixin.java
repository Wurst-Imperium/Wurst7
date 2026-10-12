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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.wurstclient.WurstClient;

@Mixin(ClientTelemetryManager.class)
public class ClientTelemetryManagerMixin
{
	/**
	 * Disables telemetry event sending if NoTelemetry is enabled.
	 */
	@ModifyReturnValue(
		method = "createEventSender()Lnet/minecraft/client/telemetry/TelemetryEventSender;",
		at = @At("RETURN"))
	private TelemetryEventSender onCreateEventSender(
		TelemetryEventSender original)
	{
		return (type, buildFunction) -> {
			if(!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled())
				original.send(type, buildFunction);
		};
	}
}
