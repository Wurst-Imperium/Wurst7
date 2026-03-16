/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.CameraTransformViewBobbingListener.CameraTransformViewBobbingEvent;
import net.wurstclient.hacks.FreecamHack;
import net.wurstclient.hacks.FullbrightHack;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements AutoCloseable
{
	@WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
			ordinal = 0))
	private void onBobView(GameRenderer instance, PoseStack matrices,
		float tickDelta, Operation<Void> original)
	{
		CameraTransformViewBobbingEvent event =
			new CameraTransformViewBobbingEvent();
		EventManager.fire(event);
		
		if(!event.isCancelled())
			original.call(instance, matrices, tickDelta);
	}
	
	@ModifyReturnValue(method = "getFov(Lnet/minecraft/client/Camera;FZ)F",
		at = @At("RETURN"))
	private float onGetFov(float original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
	
	/**
	 * This is the part that makes Liquids work.
	 */
	@WrapOperation(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;",
			ordinal = 0))
	private HitResult liquidsRaycast(Entity instance, double maxDistance,
		float tickDelta, boolean includeFluids, Operation<HitResult> original)
	{
		if(!WurstClient.INSTANCE.getHax().liquidsHack.isEnabled())
			return original.call(instance, maxDistance, tickDelta,
				includeFluids);
		
		return original.call(instance, maxDistance, tickDelta, true);
	}
	
	/**
	 * Modifies the block raycast for Freecam so that it starts from the
	 * camera position instead of the player position.
	 */
	@WrapOperation(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;",
			ordinal = 0))
	private HitResult modifyBlockRaycast(Entity player, double maxDist,
		float partialTicks, boolean includeFluids,
		Operation<HitResult> original)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(!freecam.isClickingFromCamera())
			return original.call(player, maxDist, partialTicks, includeFluids);
		
		Vec3 camStart = freecam.getCamPos(partialTicks);
		Vec3 camEnd = camStart.add(freecam.getScaledCamDir(maxDist));
		return player.level()
			.clip(new ClipContext(camStart, camEnd, ClipContext.Block.OUTLINE,
				includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
				player));
	}
	
	/**
	 * Modifies the entity raycast for Freecam so that it starts from the
	 * camera position instead of the player position.
	 */
	@WrapOperation(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
	private EntityHitResult modifyEntityRaycast(Entity instance, Vec3 start,
		Vec3 end, AABB bounds, Predicate<Entity> predicate, double maxDistSq,
		Operation<EntityHitResult> original, @Local(ordinal = 0) double maxDist)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(!freecam.isClickingFromCamera())
			return original.call(instance, start, end, bounds, predicate,
				maxDistSq);
		
		Vec3 camStart = freecam.getCamPos(1F);
		Vec3 scaledCamDir = freecam.getScaledCamDir(maxDist);
		Vec3 camEnd = camStart.add(scaledCamDir);
		AABB camBounds = EntityType.PLAYER.getDimensions()
			.makeBoundingBox(camStart).expandTowards(scaledCamDir).inflate(1);
		
		return original.call(instance, camStart, camEnd, camBounds, predicate,
			maxDistSq);
	}
	
	@WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
			ordinal = 0))
	private float onRenderWorldNauseaLerp(float delta, float start, float end,
		Operation<Float> original)
	{
		if(!WurstClient.INSTANCE.getHax().antiWobbleHack.isEnabled())
			return original.call(delta, start, end);
		
		return 0;
	}
	
	@Inject(
		method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F",
		at = @At("HEAD"),
		cancellable = true)
	private static void onGetNightVisionStrength(LivingEntity entity,
		float tickDelta, CallbackInfoReturnable<Float> cir)
	{
		FullbrightHack fullbright =
			WurstClient.INSTANCE.getHax().fullbrightHack;
		
		if(fullbright.isNightVisionActive())
			cir.setReturnValue(fullbright.getNightVisionStrength());
	}
	
	@Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
		at = @At("HEAD"),
		cancellable = true)
	private void onTiltViewWhenHurt(PoseStack matrices, float tickDelta,
		CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noHurtcamHack.isEnabled())
			ci.cancel();
	}
}
