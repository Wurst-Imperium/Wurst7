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

import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.DimensionOrBossFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.WurstClient;

@Mixin(DimensionOrBossFogModifier.class)
public class DimensionOrBossFogModifierMixin
{
	/**
	 * Removes the thick fog in the Nether and during the Ender Dragon fight,
	 * if NoFog is enabled.
	 */
	@Inject(method = "applyStartEndModifier",
		at = @At("TAIL"),
		cancellable = true)
	private void onApplyStartEndModifier(FogData data, Entity cameraEntity,
		BlockPos cameraPos, ClientWorld world, float viewDistance,
		RenderTickCounter tickCounter, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
			return;
		
		data.environmentalStart = 1000000;
		data.environmentalEnd = 1000000;
	}
}
