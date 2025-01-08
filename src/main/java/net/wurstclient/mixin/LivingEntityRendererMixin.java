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

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.wurstclient.WurstClient;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
	/**
	 * Forces the nametag to be rendered if configured in NameTags.
	 */
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/MinecraftClient;getInstance()Lnet/minecraft/client/MinecraftClient;",
		ordinal = 0),
		method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z",
		cancellable = true)
	private void shouldForceLabel(LivingEntity entity, double distanceSq,
		CallbackInfoReturnable<Boolean> cir)
	{
		// return true immediately after the distance check
		if(WurstClient.INSTANCE.getHax().nameTagsHack
			.shouldForcePlayerNametags())
			cir.setReturnValue(true);
	}
}
