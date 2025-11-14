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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.wurstclient.WurstClient;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
	/**
	 * Disables the distance limit in hasLabel() if configured in NameTags.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D",
		ordinal = 0),
		method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z")
	private double adjustDistance(EntityRenderDispatcher render, Entity entity,
		Operation<Double> original)
	{
		// pretend the distance is 1 so the check always passes
		if(WurstClient.INSTANCE.getHax().nameTagsHack.isUnlimitedRange())
			return 1;
		
		return original.call(render, entity);
	}
	
	/**
	 * Forces the nametag to be rendered if configured in NameTags.
	 */
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/Minecraft;getInstance()Lnet/minecraft/client/Minecraft;",
		ordinal = 0),
		method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
		cancellable = true)
	private void shouldForceLabel(LivingEntity entity,
		CallbackInfoReturnable<Boolean> cir)
	{
		// return true immediately after the distance check
		if(WurstClient.INSTANCE.getHax().nameTagsHack
			.shouldForcePlayerNametags())
			cir.setReturnValue(true);
	}
}
