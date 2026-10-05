/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.noclip;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin
{
	/**
	 * Skips vanilla's position packet and client-side position update if
	 * requested by NoClip. Must run after the sprinting packet.
	 */
	@ModifyExpressionValue(method = "sendPosition()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z"))
	private boolean shouldSendPosition(boolean original)
	{
		return original
			&& WurstClient.INSTANCE.getHax().noClipHack.shouldSendPosition();
	}
	
	/**
	 * Prevents the player from being pushed out of blocks when NoClip is
	 * enabled. Not required for NoClip to work, just makes the falling sand
	 * method easier.
	 */
	@Inject(method = "moveTowardsClosestSpace(DD)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onMoveTowardsClosestSpace(double x, double z, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noClipHack.isEnabled())
			ci.cancel();
	}
}
