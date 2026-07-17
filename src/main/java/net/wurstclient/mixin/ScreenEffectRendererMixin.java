/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.wurstclient.WurstClient;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin
{
	@ModifyConstant(
		method = "lambda$submitFire$0(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V",
		constant = @Constant(floatValue = -0.3F))
	private static float getFireOffset(float original)
	{
		return original - WurstClient.INSTANCE.getHax().noFireOverlayHack
			.getOverlayOffset();
	}
	
	@Inject(
		method = "submitWater(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
		at = @At("HEAD"),
		cancellable = true)
	private static void onRenderUnderwaterOverlay(Minecraft client,
		PoseStack matrices, SubmitNodeCollector submitNodeCollector,
		CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled())
			ci.cancel();
	}
}
