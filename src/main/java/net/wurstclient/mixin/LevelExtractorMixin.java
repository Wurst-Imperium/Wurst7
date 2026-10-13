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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin
{
	/**
	 * Prevents the block overlay when moving the camera into a block using
	 * CameraNoClip or Freecam.
	 */
	@Inject(
		method = "getViewBlockingState(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/world/level/block/state/BlockState;",
		at = @At("HEAD"),
		cancellable = true)
	private static void onGetViewBlockingState(LocalPlayer player,
		Frustum frustum, CallbackInfoReturnable<BlockState> cir)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax.freecamHack.isEnabled() || hax.cameraNoClipHack.isEnabled()
			&& !WurstClient.MC.options.getCameraType().isFirstPerson())
			cir.setReturnValue(null);
	}
}
