/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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

import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NoWeatherHack;

@Mixin(World.class)
public abstract class WorldMixin implements IWorld, AutoCloseable
{
	@Inject(at = {@At("HEAD")},
		method = {"getRainGradient(F)F"},
		cancellable = true)
	private void onGetRainGradient(float f, CallbackInfoReturnable<Float> cir)
	{
		if(WurstClient.INSTANCE.getHax().noWeatherHack.isRainDisabled())
			cir.setReturnValue(0F);
	}
	
	@Override
	public float getSkyAngle(float tickDelta)
	{
		NoWeatherHack noWeatherHack =
			WurstClient.INSTANCE.getHax().noWeatherHack;
		
		long timeOfDay =
			noWeatherHack.isTimeChanged() ? noWeatherHack.getChangedTime()
				: getLevelProperties().getTimeOfDay();
		
		return getDimension().getSkyAngle(timeOfDay, tickDelta);
	}
	
	@Override
	public int getMoonPhase()
	{
		NoWeatherHack noWeatherHack =
			WurstClient.INSTANCE.getHax().noWeatherHack;
		
		if(noWeatherHack.isMoonPhaseChanged())
			return noWeatherHack.getChangedMoonPhase();
		
		return getDimension().getMoonPhase(getLevelProperties().getTimeOfDay());
	}
}
