/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.portalgui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin
{
	/**
	 * Prevents portals from auto-closing the current screen if PortalGUI is
	 * enabled.
	 */
	@ModifyExpressionValue(method = "handlePortalTransitionEffect(Z)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Gui;screen()Lnet/minecraft/client/gui/screens/Screen;"))
	private Screen hideScreenFromPortalCheck(Screen original)
	{
		return WurstClient.INSTANCE.getHax().portalGuiHack.isEnabled() ? null
			: original;
	}
}
