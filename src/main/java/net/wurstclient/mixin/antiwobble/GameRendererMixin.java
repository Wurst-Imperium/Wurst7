/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiwobble;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.renderer.GameRenderer;
import net.wurstclient.WurstClient;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	/**
	 * Disables nausea and portal wobble when using AntiWobble,
	 * without the green tint that the vanilla setting creates.
	 */
	@ModifyExpressionValue(
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;screenEffectScale:F"),
		require = 1)
	private float onRenderLevelScreenEffectScale(float original)
	{
		return WurstClient.INSTANCE.getHax().antiWobbleHack.isEnabled() ? 0
			: original;
	}
}
