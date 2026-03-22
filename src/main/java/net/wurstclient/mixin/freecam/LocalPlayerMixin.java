/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer
{
	private LocalPlayerMixin(WurstClient wurst, ClientLevel world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(method = "isShiftKeyDown()Z", at = @At("HEAD"), cancellable = true)
	private void onIsShiftKeyDown(CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().freecamHack.isMovingCamera())
			cir.setReturnValue(false);
	}
	
	@Override
	public void turn(double deltaYaw, double deltaPitch)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
		{
			freecam.turn(deltaYaw, deltaPitch);
			return;
		}
		
		super.turn(deltaYaw, deltaPitch);
	}
	
	@WrapOperation(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
	private static HitResult modifyBlockRaycast(Entity player, double maxDist,
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
	
	@WrapOperation(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
	private static EntityHitResult modifyEntityRaycast(Entity instance,
		Vec3 start, Vec3 end, AABB bounds, Predicate<Entity> predicate,
		double maxDistSq, Operation<EntityHitResult> original,
		@Local(ordinal = 0) double maxDist)
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
}
