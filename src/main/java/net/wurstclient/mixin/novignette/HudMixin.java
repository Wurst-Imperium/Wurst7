/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.novignette;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;

@Mixin(Hud.class)
public class HudMixin
{
	/**
	 * Disables the darkened screen edges and world border warning tint when
	 * NoVignette is enabled.
	 */
	@Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
	private void onExtractVignette(GuiGraphicsExtractor context, Entity entity,
		CallbackInfo ci)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.noVignetteHack.isEnabled())
			return;
		
		ci.cancel();
	}
}
