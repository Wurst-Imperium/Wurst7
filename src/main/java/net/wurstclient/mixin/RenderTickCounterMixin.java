/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.render.RenderTickCounter;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.TimerHack;

@Mixin(RenderTickCounter.class)
public abstract class RenderTickCounterMixin
{
	@Shadow
	private float lastFrameDuration;
	
	@Inject(at = {@At(value = "FIELD",
		target = "Lnet/minecraft/client/render/RenderTickCounter;prevTimeMillis:J",
		opcode = Opcodes.PUTFIELD,
		ordinal = 0)}, method = {"beginRenderTick(J)V"})
	public void onBeginRenderTick(long long_1, CallbackInfo ci)
	{
		TimerHack timerHack = WurstClient.INSTANCE.getHax().timerHack;
		lastFrameDuration *= timerHack.getTimerSpeed();
	}
}
