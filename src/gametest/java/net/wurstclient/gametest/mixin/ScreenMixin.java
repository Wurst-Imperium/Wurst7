/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.CommonColors;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler
	implements Renderable
{
	@Shadow
	public int width;
	@Shadow
	public int height;
	
	/**
	 * Replaces the panorama background with a gray background to make test
	 * screenshots consistent.
	 */
	@Inject(at = @At("HEAD"), method = "renderPanorama", cancellable = true)
	public void renderPanoramaBackground(GuiGraphics context, float deltaTicks,
		CallbackInfo ci)
	{
		context.fill(0, 0, width, height, CommonColors.GRAY);
		ci.cancel();
	}
}
