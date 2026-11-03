/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.liquids;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin
{
	/**
	 * Makes Liquids work. Must take priority over Freecam's raycast wrapper.
	 */
	@ModifyArg(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;",
			ordinal = 0),
		index = 2)
	private static boolean modifyIncludeFluidsForLiquids(boolean includeFluids)
	{
		return includeFluids
			|| WurstClient.INSTANCE.getHax().liquidsHack.isEnabled();
	}
}
