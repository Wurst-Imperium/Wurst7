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

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.wurstclient.WurstClient;

@Mixin(AtmosphericFogModifier.class)
public class AtmosphericFogModifierMixin
{
	/**
	 * Removes the foggy overlay in the Overworld (including rain fog), if
	 * NoFog is enabled.
	 */
	@Inject(method = "applyStartEndModifier",
		at = @At("TAIL"),
		cancellable = true)
	private void onApplyStartEndModifier(FogData data, Camera camera,
		ClientWorld world, float viewDistance, RenderTickCounter tickCounter,
		CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
			return;
		
		data.environmentalStart = 1000000;
		data.environmentalEnd = 1000000;
	}
}
