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

import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.HealthTagsHack;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState>
{
	/**
	 * Disables the nametag distance limit if configured in NameTags.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/entity/EntityRenderManager;getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D"),
		method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V")
	private double fakeSquaredDistanceToCamera(EntityRenderManager dispatcher,
		Entity entity, Operation<Double> original,
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
		method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V")
	private void restoreSquaredDistanceToCamera(T entity, S state,
		float tickDelta, CallbackInfo ci,
		@Share("actualDistanceSq") LocalDoubleRef actualDistanceSq)
	{
		state.squaredDistanceToCamera = actualDistanceSq.get();
	}
	
	/**
	 * Modifies the display name in the render state to include health
	 * information when HealthTags is enabled. This is called every frame, so
	 * the health values are always up-to-date and automatically revert when
	 * HealthTags is disabled.
	 */
	@Inject(at = @At("TAIL"),
		method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V")
	private void addHealthToDisplayName(T entity, S state, float tickProgress,
		CallbackInfo ci)
	{
		if(state.displayName == null)
			return;
		if(!(entity instanceof LivingEntity le))
			return;
		
		HealthTagsHack healthTags =
			WurstClient.INSTANCE.getHax().healthTagsHack;
		if(!healthTags.isEnabled())
			return;
		
		state.displayName = healthTags.addHealth(le, state.displayName.copy());
	}
}
