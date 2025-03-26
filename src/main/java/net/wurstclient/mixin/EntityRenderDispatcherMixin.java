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
	 *
	 * <p>
	 * Target is the last render() method, set to private, just above
	 * addRendererDetails().
	 *
	 * <p>
	 * Method is the other private render() that calls it, the big one with
	 * tickProgress and EntityRenderer parameters and CrashReport creation
	 * logic.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/render/entity/state/EntityRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"),
		method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V")
	private <E extends Entity, S extends EntityRenderState> void wrapRender(
		EntityRenderDispatcher instance, S state, double x, double y, double z,
		MatrixStack matrices, VertexConsumerProvider vcp, int light,
		EntityRenderer<?, S> renderer, Operation<Void> original, E entity,
		double x2, double y2, double z2, float tickProgress,
		MatrixStack matrices2, VertexConsumerProvider vcp2, int light2,
		EntityRenderer<? super E, S> renderer2)
	{
		Text originalDisplayName = state.displayName;
		HealthTagsHack healthTags =
			WurstClient.INSTANCE.getHax().healthTagsHack;
		
		if(healthTags.isEnabled() && entity instanceof LivingEntity le
			&& originalDisplayName != null)
			state.displayName =
				healthTags.addHealth(le, originalDisplayName.copy());
		
		original.call(instance, state, x, y, z, matrices, vcp, light, renderer);
		state.displayName = originalDisplayName;
	}
}
