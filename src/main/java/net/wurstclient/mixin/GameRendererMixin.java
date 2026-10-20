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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.CameraTransformViewBobbingListener.CameraTransformViewBobbingEvent;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements AutoCloseable
{
	/**
	 * Prevents view bobbing when hacks disable it.
	 */
	@WrapOperation(method = "renderLevel()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
			ordinal = 0))
	private void onBobView(GameRenderer instance, CameraRenderState cameraState,
		PoseStack matrices, Operation<Void> original)
	{
		CameraTransformViewBobbingEvent event =
			new CameraTransformViewBobbingEvent();
		EventManager.fire(event);
		
		if(!event.isCancelled())
			original.call(instance, cameraState, matrices);
	}
}
