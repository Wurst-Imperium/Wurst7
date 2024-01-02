/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.GUIRenderListener.GUIRenderEvent;

@Mixin(InGameHud.class)
public class IngameHudMixin
{
	@Shadow
	@Final
	private DebugHud debugHud;
	
	@Inject(
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V",
			remap = false,
			ordinal = 3),
		method = "render(Lnet/minecraft/client/gui/DrawContext;F)V")
	private void onRender(DrawContext context, float tickDelta, CallbackInfo ci)
	{
		if(debugHud.shouldShowDebugHud())
			return;
		
		EventManager.fire(new GUIRenderEvent(context, tickDelta));
	}
	
	@Inject(at = @At("HEAD"),
		method = "renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V",
		cancellable = true)
	private void onRenderOverlay(DrawContext context, Identifier texture,
		float opacity, CallbackInfo ci)
	{
		if(texture == null
			|| !"textures/misc/pumpkinblur.png".equals(texture.getPath()))
			return;
		
		if(WurstClient.INSTANCE.getHax().noPumpkinHack.isEnabled())
			ci.cancel();
	}
}
