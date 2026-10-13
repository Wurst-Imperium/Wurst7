/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nofireoverlay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.wurstclient.WurstClient;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin
{
	/**
	 * Lowers both sides of the first-person fire overlay by the offset supplied
	 * by NoFireOverlay.
	 */
	@ModifyConstant(
		method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
		constant = @Constant(floatValue = -0.3F))
	private static float getFireOffset(float original)
	{
		return original - WurstClient.INSTANCE.getHax().noFireOverlayHack
			.getOverlayOffset();
	}
}
