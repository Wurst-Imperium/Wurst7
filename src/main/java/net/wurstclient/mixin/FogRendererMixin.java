/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.wurstclient.WurstClient;

@Mixin(FogRenderer.class)
public class FogRendererMixin
{
	/**
	 * Removes the fog near the end of the render distance in all dimensions, if
	 * NoFog is enabled.
	 */
	@ModifyReturnValue(at = @At("RETURN"),
		method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/renderer/fog/FogData;")
	private FogData modifyFogData(FogData fog)
	{
		if(WurstClient.INSTANCE.getHax().noFogHack.isEnabled())
		{
			fog.renderDistanceStart = 1000000;
			fog.renderDistanceEnd = 1000000;
		}
		
		return fog;
	}
}
