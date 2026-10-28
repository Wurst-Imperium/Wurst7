/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.flight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;

@Mixin(Player.class)
public abstract class PlayerMixin
{
	/**
	 * Prevents swimming from adding a slight vertical drift to Flight's
	 * movement when you look up or down while trying to fly horizontally.
	 */
	@Inject(method = "updateSwimming()V", at = @At("HEAD"), cancellable = true)
	private void onUpdateSwimming(CallbackInfo ci)
	{
		if((Object)this != WurstClient.MC.player
			|| !WurstClient.INSTANCE.getHax().flightHack.isEnabled())
			return;
		
		WurstClient.MC.player.setSwimming(false);
		ci.cancel();
	}
}
