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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.HealthTagsHack;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>
{
	/**
	 * Disables the nametag distance limit if configured in NameTags.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"),
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V")
	private double fakeSquaredDistanceToCamera(
		EntityRenderDispatcher dispatcher, Entity entity,
		Operation<Double> original,
		@Share("actualDistanceSq") LocalDoubleRef actualDistanceSq)
	{
		actualDistanceSq.set(original.call(dispatcher, entity));
		
		if(WurstClient.INSTANCE.getHax().nameTagsHack.isUnlimitedRange())
			return 0;
		
		return actualDistanceSq.get();
	}
	
	/**
	 * Restores the true squared distance so we don't break other code that
	 * might rely on it.
	 */
	@Inject(at = @At("TAIL"),
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V")
	private void restoreSquaredDistanceToCamera(T entity, S state,
		float tickDelta, CallbackInfo ci,
		@Share("actualDistanceSq") LocalDoubleRef actualDistanceSq)
	{
		state.distanceToCameraSq = actualDistanceSq.get();
	}
	
	/**
	 * Modifies the display name in the render state to include health
	 * information when HealthTags is enabled. This is called every frame, so
	 * the health values are always up-to-date and automatically revert when
	 * HealthTags is disabled.
	 */
	@Inject(at = @At("TAIL"),
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V")
	private void addHealthToDisplayName(T entity, S state, float tickProgress,
		CallbackInfo ci)
	{
		if(state.nameTag == null)
			return;
		if(!(entity instanceof LivingEntity le))
			return;
		
		HealthTagsHack healthTags =
			WurstClient.INSTANCE.getHax().healthTagsHack;
		if(!healthTags.isEnabled())
			return;
		
		state.nameTag = healthTags.addHealth(le, state.nameTag.copy());
	}
}
