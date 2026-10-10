/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.cameradistance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.Camera;
import net.wurstclient.WurstClient;

@Mixin(Camera.class)
public abstract class CameraMixin
{
	@ModifyVariable(method = "getMaxZoom(F)F",
		at = @At("HEAD"),
		argsOnly = true)
	private float changeMaxCameraDistance(float original)
	{
		return WurstClient.INSTANCE.getHax().cameraDistanceHack
			.getDistance(original);
	}
}
