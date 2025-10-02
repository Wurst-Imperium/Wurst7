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

import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.RenderBlockEntityListener.RenderBlockEntityEvent;

@Mixin(BlockEntityRenderManager.class)
public class BlockEntityRenderDispatcherMixin
{
	@Inject(at = @At("HEAD"),
		method = "render(Lnet/minecraft/client/render/block/entity/state/BlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
		cancellable = true)
	private <S extends BlockEntityRenderState> void onRenderRenderState(
		S renderState, MatrixStack matrices, OrderedRenderCommandQueue queue,
		CameraRenderState cameraRenderState, CallbackInfo ci)
	{
		RenderBlockEntityEvent event = new RenderBlockEntityEvent(renderState);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
}
