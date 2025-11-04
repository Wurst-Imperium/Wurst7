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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeAccess;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.WorldEnvironmentAttributeAccess;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NoWeatherHack;

@Mixin(WorldEnvironmentAttributeAccess.class)
public abstract class WorldEnvironmentAttributeAccessMixin
	implements EnvironmentAttributeAccess
{
	@ModifyReturnValue(at = @At("RETURN"),
		method = {
			"getAttributeValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;)Ljava/lang/Object;",
			"getAttributeValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/attribute/WeightedAttributeList;)Ljava/lang/Object;"},
		require = 2)
	public Object onGetAttributeValue(Object original,
		EnvironmentAttribute<?> attribute)
	{
		NoWeatherHack noWeather = WurstClient.INSTANCE.getHax().noWeatherHack;
		
		if(attribute == EnvironmentAttributes.MOON_PHASE_VISUAL
			&& noWeather.isMoonPhaseChanged())
			return noWeather.getChangedMoonPhase();
		
		return original;
	}
}
