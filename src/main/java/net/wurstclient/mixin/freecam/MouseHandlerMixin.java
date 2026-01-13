/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin
{
	/**
	 * Prevents scrolling in Freecam from changing the player's
	 * flying speed attribute.
	 */
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"),
		method = "onScroll(JDD)V")
	private boolean wrapIsSpectatorInOnScroll(LocalPlayer player,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().freecamHack.isEnabled())
			return false;
		
		return original.call(player);
	}
}
