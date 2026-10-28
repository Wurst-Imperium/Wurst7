/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.flight;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.wurstclient.WurstClient;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
	/**
	 * Prevents travelInFluid() from changing Flight's movement.
	 */
	@ModifyReturnValue(
		method = "shouldTravelInFluid(Lnet/minecraft/world/level/material/FluidState;)Z",
		at = @At("RETURN"))
	private boolean modifyShouldTravelInFluid(boolean original)
	{
		return original && !((Object)this == WurstClient.MC.player
			&& WurstClient.INSTANCE.getHax().flightHack.isEnabled());
	}
	
	/**
	 * Prevents fluid jumping from adding to Flight's vertical movement.
	 */
	@ModifyExpressionValue(method = "aiStep()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityFluidInteraction;getFluidsToJumpIn(D)Ljava/util/List;"))
	private List<Holder<Fluid>> modifyFluidsToJumpIn(List<Holder<Fluid>> fluids)
	{
		if((Object)this == WurstClient.MC.player
			&& WurstClient.INSTANCE.getHax().flightHack.isEnabled())
			return List.of();
		
		return fluids;
	}
}
