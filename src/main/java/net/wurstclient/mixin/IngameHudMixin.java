/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.events.GUIRenderListener.GUIRenderEvent;

@Mixin(InGameHud.class)
public class IngameHudMixin extends DrawableHelper
{
	@Inject(
		at = {@At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V",
			ordinal = 4)},
		method = {"render(Lnet/minecraft/client/util/math/MatrixStack;F)V"})
	private void onRender(MatrixStack matrixStack, float partialTicks,
		CallbackInfo ci)
	{
		if(WurstClient.MC.options.debugEnabled)
			return;
		
		GUIRenderEvent event = new GUIRenderEvent(matrixStack, partialTicks);
		WurstClient.INSTANCE.getEventManager().fire(event);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"renderPumpkinOverlay()V"},
		cancellable = true)
	private void onRenderPumpkinOverlay(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noPumpkinHack.isEnabled())
			ci.cancel();
	}
}
