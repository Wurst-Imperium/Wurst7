/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.wurstclient.WurstClient;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z",
		ordinal = 0),
		method = {
			"render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"})
	private boolean canWurstSeePlayer(LivingEntity e, PlayerEntity player)
	{
		if(WurstClient.INSTANCE.getHax().trueSightHack.isEnabled())
			return false;
		
		return e.isInvisibleTo(player);
	}

	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;getSquaredDistanceToCamera(Lnet/minecraft/entity/Entity;)D",
		ordinal = 0),
		method = {"hasLabel(Lnet/minecraft/entity/LivingEntity;)Z"})
	private double adjustDistance(EntityRenderDispatcher render, Entity entity)
	{
		if(WurstClient.INSTANCE.getHax().nameTagsHack.isUnlimitedRange())
			return 1;

		return render.getSquaredDistanceToCamera(entity);
	}

	@Inject(at = {@At(value = "INVOKE",
		target = "Lnet/minecraft/client/MinecraftClient;getInstance()Lnet/minecraft/client/MinecraftClient;",
		ordinal = 0)},
		method = {
			"hasLabel(Lnet/minecraft/entity/LivingEntity;)Z"},
		cancellable = true)
	private void shouldForceLabel(LivingEntity e, CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().nameTagsHack.alwaysVisibleNametags())
			cir.setReturnValue(true);
	}
}
