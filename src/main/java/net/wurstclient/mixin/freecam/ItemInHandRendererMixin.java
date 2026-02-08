/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.wurstclient.WurstClient;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin
{
	/**
	 * Makes the "Hide hand" setting in Freecam work.
	 */
	@Inject(at = @At("HEAD"),
		method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
		cancellable = true)
	private void onRenderHandsWithItems(float tickProgress, PoseStack matrices,
		SubmitNodeCollector entityRenderCommandQueue, LocalPlayer player,
		int light, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().freecamHack.shouldHideHand())
			ci.cancel();
	}
}
