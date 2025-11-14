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
		target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V"),
		method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V")
	private <E extends Entity, S extends EntityRenderState> void wrapRender(
		EntityRenderDispatcher instance, S state, double x, double y, double z,
		PoseStack matrices, MultiBufferSource vcp, int light,
		EntityRenderer<?, S> renderer, Operation<Void> original, E entity,
		double x2, double y2, double z2, float tickProgress,
		PoseStack matrices2, MultiBufferSource vcp2, int light2,
		EntityRenderer<? super E, S> renderer2)
	{
		Component originalDisplayName = state.nameTag;
		HealthTagsHack healthTags =
			WurstClient.INSTANCE.getHax().healthTagsHack;
		
		if(healthTags.isEnabled() && entity instanceof LivingEntity le
			&& originalDisplayName != null)
			state.nameTag =
				healthTags.addHealth(le, originalDisplayName.copy());
		
		original.call(instance, state, x, y, z, matrices, vcp, light, renderer);
		state.nameTag = originalDisplayName;
	}
}
