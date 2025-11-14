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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.HealthTagsHack;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
	implements ResourceManagerReloadListener
{
	/**
	 * Temporarily replaces an entity's display name to make HealthTags work.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
		method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V")
	private <E extends Entity, S extends EntityRenderState> void wrapRender(
		EntityRenderer<? super E, S> renderer, S state, PoseStack matrices,
		MultiBufferSource vertexConsumers, int light, Operation<Void> original,
		E entity, double x, double y, double z, float tickDelta,
		PoseStack matrices2, MultiBufferSource vertexConsumers2, int light2,
		EntityRenderer<? super E, S> renderer2)
	{
		Component originalDisplayName = state.nameTag;
		HealthTagsHack healthTags =
			WurstClient.INSTANCE.getHax().healthTagsHack;
		
		if(healthTags.isEnabled() && entity instanceof LivingEntity le
			&& originalDisplayName != null)
			state.nameTag =
				healthTags.addHealth(le, originalDisplayName.copy());
		
		original.call(renderer, state, matrices, vertexConsumers, light);
		state.nameTag = originalDisplayName;
	}
}
