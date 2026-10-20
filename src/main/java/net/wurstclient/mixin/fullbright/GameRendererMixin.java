/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.fullbright;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	@Inject(
		method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F",
		at = @At("HEAD"),
		cancellable = true)
	private static void onGetNightVisionScale(LivingEntity entity,
		float tickDelta, CallbackInfoReturnable<Float> cir)
	{
		float nightVisionStrength = WurstClient.INSTANCE.getHax().fullbrightHack
			.getNightVisionStrength();
		
		if(nightVisionStrength > 0)
			cir.setReturnValue(nightVisionStrength);
	}
}
