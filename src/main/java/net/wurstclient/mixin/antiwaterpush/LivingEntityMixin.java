/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiwaterpush;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
	/**
	 * Removes the underwater walking slowdown when AntiWaterPush is enabled.
	 */
	@ModifyExpressionValue(
		method = "travelInWater(Lnet/minecraft/world/phys/Vec3;DZD)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double modifyWaterMovementEfficiency(double original)
	{
		if((Object)this == WurstClient.MC.player
			&& WurstClient.INSTANCE.getHax().antiWaterPushHack
				.isPreventingSlowdown())
			return 1;
		
		return original;
	}
}
