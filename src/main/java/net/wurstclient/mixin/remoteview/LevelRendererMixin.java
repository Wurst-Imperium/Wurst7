/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.remoteview;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.RemoteViewHack;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
	/**
	 * Keeps the player model visible while using RemoteView outside of
	 * spectator mode.
	 */
	@Definition(id = "LocalPlayer", type = LocalPlayer.class)
	@Expression("? instanceof LocalPlayer")
	@ModifyExpressionValue(
		method = "extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/LevelRenderState;)V",
		at = @At("MIXINEXTRAS:EXPRESSION"))
	private boolean modifyLocalPlayerExclusion(boolean original)
	{
		RemoteViewHack remoteView =
			WurstClient.INSTANCE.getHax().remoteViewHack;
		LocalPlayer player = WurstClient.MC.player;
		return original && !(remoteView.isViewingEntity() && player != null
			&& !player.isSpectator());
	}
}
