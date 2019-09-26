/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
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

import net.minecraft.block.BlockRenderLayer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.wurstclient.WurstClient;
import net.wurstclient.events.RenderBlockEntityListener.RenderBlockEntityEvent;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin
{
	@Inject(at = {@At("HEAD")},
		method = {
			"render(Lnet/minecraft/block/entity/BlockEntity;FILnet/minecraft/block/BlockRenderLayer;Lnet/minecraft/client/render/BufferBuilder;)V"},
		cancellable = true)
	private void onRender(BlockEntity blockEntity, float partialTicks,
		int destroyStage, BlockRenderLayer blockRenderLayer_1,
		BufferBuilder bufferBuilder_1, CallbackInfo ci)
	{
		RenderBlockEntityEvent event = new RenderBlockEntityEvent(blockEntity);
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
}
