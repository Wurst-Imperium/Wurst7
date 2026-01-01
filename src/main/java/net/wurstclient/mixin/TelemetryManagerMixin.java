/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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

import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.wurstclient.WurstClient;

@Mixin(ClientTelemetryManager.class)
public class TelemetryManagerMixin
{
	@Inject(at = @At("HEAD"),
		method = "getOutsideSessionSender()Lnet/minecraft/client/telemetry/TelemetryEventSender;",
		cancellable = true)
	private void onGetSender(CallbackInfoReturnable<TelemetryEventSender> cir)
	{
		if(!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled())
			return;
		
		// Return a dummy that can't actually send anything. :)
		cir.setReturnValue(TelemetryEventSender.DISABLED);
	}
}
