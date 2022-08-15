/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import net.wurstclient.WurstClient;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin
{
	@Inject(at = {@At("HEAD")},
		method = {
			"getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;"},
		cancellable = true)
	private static void onGetFogModifier(Entity entity, float tickDelta,
		CallbackInfoReturnable<StatusEffectFogModifier> ci)
	{
		if(WurstClient.INSTANCE.getHax().antiBlindHack.isEnabled())
			ci.setReturnValue(null);
	}
}
