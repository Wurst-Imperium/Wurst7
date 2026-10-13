/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nooverlay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.wurstclient.WurstClient;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin
{
	/**
	 * Disables the (very subtle) underwater texture overlay when NoOverlay is
	 * enabled.
	 */
	@Inject(
		method = "renderWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
		at = @At("HEAD"),
		cancellable = true)
	private static void onRenderWater(Minecraft client, PoseStack matrices,
		MultiBufferSource vertexConsumerProvider, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled())
			ci.cancel();
	}
}
