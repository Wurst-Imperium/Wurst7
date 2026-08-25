/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.autosprint;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin
{
	/**
	 * This mixin makes AutoSprint's "Omnidirectional Sprint" setting work.
	 */
	@WrapOperation(method = "shouldStopRunSprinting()Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
	private boolean wrapHasForwardMovement(ClientInput input,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().autoSprintHack.shouldOmniSprint())
			return input.getMoveVector().length() > 1e-5F;
		
		return original.call(input);
	}
	
	/**
	 * This mixin allows AutoSprint to enable sprinting even when the player is
	 * too hungry.
	 */
	@Inject(method = "isSprintingPossible(Z)Z",
		at = @At("HEAD"),
		cancellable = true)
	private void onCanSprint(boolean allowTouchingWater,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().autoSprintHack.shouldSprintHungry())
			cir.setReturnValue(true);
	}
}
