/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.RenderTickCounter;
import net.wurstclient.WurstClient;

@Mixin(RenderTickCounter.class)
public abstract class RenderTickCounterMixin
{
	@Shadow
	public float lastFrameDuration;
	
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/render/RenderTickCounter;prevTimeMillis:J",
		opcode = Opcodes.PUTFIELD,
		ordinal = 0), method = "beginRenderTick(J)I")
	public void onBeginRenderTick(long timeMillis,
		CallbackInfoReturnable<Integer> cir)
	{
		lastFrameDuration *=
			WurstClient.INSTANCE.getHax().timerHack.getTimerSpeed();
	}
}
