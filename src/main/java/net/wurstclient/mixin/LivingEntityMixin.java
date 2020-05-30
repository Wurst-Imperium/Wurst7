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

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.wurstclient.WurstClient;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
	@Inject(at = {@At("HEAD")},
		method = {
			"hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"},
		cancellable = true)
	private void hasNightVisionStatus(StatusEffect effect, CallbackInfoReturnable<Boolean> cir)
	{
		LivingEntity en = (LivingEntity)(Object)this;
		if(effect == StatusEffects.NIGHT_VISION
			&& WurstClient.INSTANCE.getHax().fullbrightHack.isNightVisionActive()
			&& en instanceof ClientPlayerEntity
			&& en.getY() > 0)
			cir.setReturnValue(true);
	}
}
