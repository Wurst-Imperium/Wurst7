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
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.wurstclient.WurstClient;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractParentElement implements Drawable
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screen/Screen;fillGradient(IIIIII)V",
		ordinal = 0),
		method = {"renderBackground(I)V"})
	private void onFillGradient(Screen screen, int top, int left, int right, int bottom, int color1, int color2)
	{
		if(WurstClient.INSTANCE.getHax().noGuiBackgroundHack.isEnabled())
			fillGradient(top, left, right, bottom, 0x00101010, 0x00101010);
		else
			fillGradient(top, left, right, bottom, color1, color2);
	}
}
