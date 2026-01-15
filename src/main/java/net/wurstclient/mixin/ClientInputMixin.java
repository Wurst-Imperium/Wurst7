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

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;

@Mixin(ClientInput.class)
public class ClientInputMixin
{
	@Inject(at = @At("HEAD"),
		method = "getMoveVector()Lnet/minecraft/world/phys/Vec2;",
		cancellable = true)
	private void onGetMoveVector(CallbackInfoReturnable<Vec2> cir)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam != null && freecam.isBaritoneMode()
			&& !freecam.isBaritonePathing())
			cir.setReturnValue(Vec2.ZERO);
	}
	
	@Inject(at = @At("HEAD"),
		method = "hasForwardImpulse()Z",
		cancellable = true)
	private void onHasForwardImpulse(CallbackInfoReturnable<Boolean> cir)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam != null && freecam.isBaritoneMode()
			&& !freecam.isBaritonePathing())
			cir.setReturnValue(false);
	}
}
