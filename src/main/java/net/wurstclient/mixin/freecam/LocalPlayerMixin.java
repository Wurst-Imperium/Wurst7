/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.freecam;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.FreecamHack;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer
{
	@Shadow
	public ClientInput input;
	
	@Unique
	private ClientInput realInput;
	
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
	
	@Inject(method = "aiStep()V", at = @At("HEAD"))
	private void onAiStepHead(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().freecamHack.isMovingCamera())
			return;
		
		realInput = input;
		input.tick();
		input = new ClientInput();
	}
	
	@Inject(method = "aiStep()V", at = @At("RETURN"))
	private void onAiStepReturn(CallbackInfo ci)
	{
		if(realInput == null)
			return;
		
		input = realInput;
		realInput = null;
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
	
	@Inject(at = @At("HEAD"),
		method = "raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;",
		cancellable = true)
	private void onRaycastHitResult(float partialTicks, Entity entity,
		CallbackInfoReturnable<HitResult> cir)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
			cir.setReturnValue(LocalPlayer.pick(entity, blockInteractionRange(),
				entityInteractionRange(), partialTicks));
	}
	
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"),
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;")
	private static Vec3 modifyViewVectorForPick(Entity entity,
		float partialTicks, Operation<Vec3> original)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
			return freecam.getCamViewVec();
		
		return original.call(entity, partialTicks);
	}
	
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"),
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;")
	private static AABB modifyBoundingBoxForPick(Entity entity,
		Operation<AABB> original)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
			return freecam.getCamBoundingBox();
		
		return original.call(entity);
	}
	
	@Override
	public @NotNull HitResult pick(double maxDistance, float partialTicks,
		boolean includeFluids)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
			return freecam.raytrace(maxDistance, partialTicks, includeFluids);
		
		return super.pick(maxDistance, partialTicks, includeFluids);
	}
}
