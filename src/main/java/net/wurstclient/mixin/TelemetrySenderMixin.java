/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.util.telemetry.TelemetrySender;
import net.minecraft.client.util.telemetry.TelemetrySender.PlayerGameMode;
import net.wurstclient.WurstClient;

@Mixin(TelemetrySender.class)
public class TelemetrySenderMixin
{
	@Shadow
	private boolean sent;
	
	@Inject(at = @At("HEAD"),
		method = "send(Lnet/minecraft/client/util/telemetry/TelemetrySender$PlayerGameMode;)V",
		cancellable = true)
	private void onSend(PlayerGameMode gameMode, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled())
			return;
			
		// Pretend it was sent successfully so that the TelemetrySender
		// won't try again later.
		sent = true;
		
		// Don't actually send anything. :)
		ci.cancel();
		
		System.out.println("Telemetry sending attempt blocked.");
	}
}
