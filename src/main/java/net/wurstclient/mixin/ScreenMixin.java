/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.wurstclient.WurstClient;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler
	implements Renderable
{
	@Inject(at = @At("HEAD"),
		method = "renderTransparentBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
		cancellable = true)
	public void onRenderInGameBackground(GuiGraphics context, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noBackgroundHack
			.shouldCancelBackground((Screen)(Object)this))
			ci.cancel();
	}
}
