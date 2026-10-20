/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidIds;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
	/**
	 * Prevents water jumping from interfering with Flight or AntiWaterPush's
	 * "Prevent slowdown" setting.
	 */
	@ModifyExpressionValue(method = "aiStep()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityFluidInteraction;getFluidsToJumpIn(D)Ljava/util/List;"))
	private List<Holder<Fluid>> modifyFluidsToJumpIn(List<Holder<Fluid>> fluids)
	{
		if((Object)this != WurstClient.MC.player
			|| !WurstClient.INSTANCE.isEnabled())
			return fluids;
		
		HackList hax = WurstClient.INSTANCE.getHax();
		if(!hax.flightHack.isEnabled()
			&& !hax.antiWaterPushHack.isPreventingSlowdown())
			return fluids;
		
		return fluids.stream().filter(fluid -> !fluid.is(FluidIds.WATER))
			.toList();
	}
}
