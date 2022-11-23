/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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

import net.minecraft.class_7965;
import net.minecraft.client.util.TelemetrySender;
import net.wurstclient.WurstClient;

@Mixin(TelemetrySender.class)
public class TelemetrySenderMixin
{
	@Inject(at = @At("HEAD"),
		method = "method_47707()Lnet/minecraft/class_7965;",
		cancellable = true)
	private void onMethod_47707(CallbackInfoReturnable<class_7965> cir)
	{
		if(!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled())
			return;
		
		// Return a dummy that can't actually send anything. :)
		cir.setReturnValue(class_7965.field_41434);
	}
}
