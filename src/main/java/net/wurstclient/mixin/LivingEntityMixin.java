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

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
	/**
	 * Stops the other darkness effect in caves when AntiBlind is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F",
		cancellable = true)
	private void onGetEffectFadeFactor(Holder<MobEffect> registryEntry,
		float delta, CallbackInfoReturnable<Float> cir)
	{
		if(registryEntry != MobEffects.DARKNESS)
			return;
		
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			cir.setReturnValue(0F);
	}
}
