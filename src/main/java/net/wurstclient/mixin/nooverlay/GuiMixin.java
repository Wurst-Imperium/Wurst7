/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nooverlay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.wurstclient.WurstClient;

@Mixin(Gui.class)
public class GuiMixin
{
	/**
	 * Disables the powder snow freezing outline when NoOverlay is enabled.
	 */
	@Inject(
		method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onRenderTextureOverlay(GuiGraphics context, Identifier texture,
		float opacity, CallbackInfo ci)
	{
		if(texture == null)
			return;
		
		if("textures/misc/powder_snow_outline.png".equals(texture.getPath())
			&& WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled())
			ci.cancel();
	}
}
