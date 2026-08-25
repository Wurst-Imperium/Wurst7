/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.healthtags;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
	 * Modifies the display name in the render state to include health
	 * information when HealthTags is enabled. This is called every frame, so
	 * the health values are always up-to-date and automatically revert when
	 * HealthTags is disabled.
	 */
	@Inject(
		method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
		at = @At("TAIL"))
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
