/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nobackground;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.wurstclient.WurstClient;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler
	implements Renderable
{
	@Inject(
		method = "renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
		at = @At("HEAD"),
		cancellable = true)
	public void onRenderTransparentBackground(GuiGraphics context,
		CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noBackgroundHack
			.shouldCancelBackground((Screen)(Object)this))
			ci.cancel();
	}
	
	@Inject(
		method = "renderBlurredBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
		at = @At("HEAD"),
		cancellable = true)
	public void onRenderBlurredBackground(GuiGraphics context, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noBackgroundHack
			.shouldCancelBackground((Screen)(Object)this))
			ci.cancel();
	}
	
	@Inject(
		method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V",
		at = @At("HEAD"),
		cancellable = true)
	public void onRenderMenuBackground(GuiGraphics context, int x, int y,
		int width, int height, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noBackgroundHack
			.shouldCancelBackground((Screen)(Object)this))
			ci.cancel();
	}
}
