/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.wurstclient.WurstClient;

@Mixin(Camera.class)
public abstract class CameraMixin
{
	@Inject(at = {@At("HEAD")},
		method = {"clipToSpace(D)D"},
		cancellable = true)
	private void onClipToSpace(double desiredCameraDistance,
		CallbackInfoReturnable<Double> cir)
	{
		if(WurstClient.INSTANCE.getHax().cameraNoClipHack.isEnabled())
			cir.setReturnValue(desiredCameraDistance);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"getSubmersionType()Lnet/minecraft/client/render/CameraSubmersionType;"},
		cancellable = true)
	private void onGetSubmersionType(
		CallbackInfoReturnable<CameraSubmersionType> cir)
	{
		if(WurstClient.INSTANCE.getHax().noOverlayHack.isEnabled())
			cir.setReturnValue(CameraSubmersionType.NONE);
	}
}
