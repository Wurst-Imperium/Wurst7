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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NoWeatherHack;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin
{
	/**
	 * Modifies the total ticks returned for the overworld clock when NoWeather
	 * is changing the time. This affects all timeline-based environment
	 * attributes including sun/moon/star angles, sky colors, fog, etc.
	 */
	@ModifyReturnValue(at = @At("RETURN"), method = "getTotalTicks")
	private long onGetTotalTicks(long original, Holder<WorldClock> definition)
	{
		NoWeatherHack noWeather = WurstClient.INSTANCE.getHax().noWeatherHack;
		
		if(!noWeather.isTimeChanged())
			return original;
		
		// Only modify the overworld clock
		if(!definition.is(WorldClocks.OVERWORLD))
			return original;
		
		// Replace the time-of-day while keeping the day number
		long dayNumber = original / 24000;
		return dayNumber * 24000 + noWeather.getChangedTime();
	}
}
