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

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.wurstclient.WurstClient;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
	/**
	 * Stops the other darkness effect in caves when AntiBlind is enabled.
	 */
	@Inject(at = @At("HEAD"),
		method = "getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F",
		cancellable = true)
	private void onGetEffectFadeFactor(
		RegistryEntry<StatusEffect> registryEntry, float delta,
		CallbackInfoReturnable<Float> cir)
	{
		if(registryEntry != StatusEffects.DARKNESS)
			return;
		
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			cir.setReturnValue(0F);
	}
}
