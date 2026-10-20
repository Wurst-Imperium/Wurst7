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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.GUIRenderListener.GUIRenderEvent;

@Mixin(Hud.class)
public class HudMixin
{
	/**
	 * Fires GUIRenderEvent after the scoreboard and before the tab list is
	 * rendered.
	 */
	@Inject(
		method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
	private void onExtractRenderState(GuiGraphicsExtractor context,
		CallbackInfo ci)
	{
		if(WurstClient.MC.debugEntries.isOverlayVisible())
			return;
		
		float tickDelta =
			context.deltaTracker().getGameTimeDeltaPartialTick(true);
		EventManager.fire(new GUIRenderEvent(context, tickDelta));
	}
}
