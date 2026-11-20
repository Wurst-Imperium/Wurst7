/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.reach;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.player.Player;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;

@Mixin(Player.class)
public abstract class PlayerMixin
{
	@ModifyReturnValue(
		method = {"blockInteractionRange()D", "entityInteractionRange()D"},
		at = @At("RETURN"))
	private double modifyInteractionRange(double original)
	{
		if((Object)this != WurstClient.MC.player)
			return original;
		
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return original;
		
		return hax.reachHack.getReachDistance();
	}
}
