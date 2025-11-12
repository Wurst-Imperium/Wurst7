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

import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Colors;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement
	implements Drawable
{
	@Shadow
	public int width;
	@Shadow
	public int height;
	
	/**
	 * Replaces the panorama background with a gray background to make test
	 * screenshots consistent.
	 */
	@Inject(at = @At("HEAD"),
		method = "renderPanoramaBackground",
		cancellable = true)
	public void renderPanoramaBackground(DrawContext context, float deltaTicks,
		CallbackInfo ci)
	{
		context.fill(0, 0, width, height, Colors.GRAY);
		ci.cancel();
	}
}
