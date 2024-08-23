/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.HealthTagsHack;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin
	implements SynchronousResourceReloader
{
	/**
	 * Temporarily replaces an entity's display name to make HealthTags work.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/entity/EntityRenderer;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"),
		method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V")
	private <E extends Entity, S extends EntityRenderState> void wrapRender(
		EntityRenderer<? super E, S> renderer, S state, MatrixStack matrices,
		VertexConsumerProvider vertexConsumers, int light,
		Operation<Void> original, E entity, double x, double y, double z,
		float tickDelta, MatrixStack matrices2,
		VertexConsumerProvider vertexConsumers2, int light2,
		EntityRenderer<? super E, S> renderer2)
	{
		Text originalDisplayName = state.displayName;
		HealthTagsHack healthTags =
			WurstClient.INSTANCE.getHax().healthTagsHack;
		
		if(healthTags.isEnabled() && entity instanceof LivingEntity le
			&& originalDisplayName != null)
			state.displayName =
				healthTags.addHealth(le, originalDisplayName.copy());
		
		original.call(renderer, state, matrices, vertexConsumers, light);
		state.displayName = originalDisplayName;
	}
}
