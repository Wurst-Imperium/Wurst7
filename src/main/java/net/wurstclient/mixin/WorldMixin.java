/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NoWeatherHack;

@Mixin(Level.class)
public abstract class WorldMixin implements LevelAccessor, AutoCloseable
{
	@Inject(at = @At("HEAD"), method = "getRainLevel(F)F", cancellable = true)
	private void onGetRainGradient(float delta,
		CallbackInfoReturnable<Float> cir)
	{
		if(WurstClient.INSTANCE.getHax().noWeatherHack.isRainDisabled())
			cir.setReturnValue(0F);
	}
	
	@ModifyReturnValue(at = @At("RETURN"), method = "getDayTime()J")
	public long onGetTimeOfDay(long original)
	{
		NoWeatherHack noWeather = WurstClient.INSTANCE.getHax().noWeatherHack;
		return noWeather.isTimeChanged() ? noWeather.getChangedTime()
			: original;
	}
}
