/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.nohurtcam;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.wurstclient.WurstClient;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
	@Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onBobHurt(PoseStack matrices, float tickDelta, CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noHurtcamHack.isEnabled())
			ci.cancel();
	}
}
