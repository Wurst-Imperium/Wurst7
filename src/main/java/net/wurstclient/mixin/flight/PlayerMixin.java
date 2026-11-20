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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;

@Mixin(Player.class)
public abstract class PlayerMixin
{
	/**
	 * Prevents flying up with Flight activating elytra.
	 */
	@ModifyReturnValue(method = "canGlide()Z", at = @At("RETURN"))
	private boolean modifyCanGlide(boolean original)
	{
		if((Object)this != WurstClient.MC.player)
			return original;
		
		return original
			&& !WurstClient.INSTANCE.getHax().flightHack.isEnabled();
	}
}
