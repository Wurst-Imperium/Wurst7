/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
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

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.CameraTransformViewBobbingListener.CameraTransformViewBobbingEvent;
import net.wurstclient.events.RenderListener.RenderEvent;
import net.wurstclient.hacks.FullbrightHack;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements AutoCloseable
{
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
		ordinal = 0),
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
	private void onBobView(GameRenderer instance, PoseStack matrices,
		float tickDelta, Operation<Void> original)
	{
		CameraTransformViewBobbingEvent event =
			new CameraTransformViewBobbingEvent();
		EventManager.fire(event);
		
		if(!event.isCancelled())
			original.call(instance, matrices, tickDelta);
	}
	
	@Inject(
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
			opcode = Opcodes.GETFIELD,
			ordinal = 0),
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
	private void onRenderWorldHandRendering(DeltaTracker tickCounter,
		CallbackInfo ci, @Local(ordinal = 1) Matrix4f matrix4f2,
		@Local(ordinal = 1) float tickDelta)
	{
		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(matrix4f2);
		RenderEvent event = new RenderEvent(matrixStack, tickDelta);
		EventManager.fire(event);
	}
	
	@ModifyReturnValue(at = @At("RETURN"),
		method = "getFov(Lnet/minecraft/client/Camera;FZ)D")
	private double onGetFov(double original)
	{
		return WurstClient.INSTANCE.getOtfs().zoomOtf
			.changeFovBasedOnZoom(original);
	}
	
	/**
	 * This is the part that makes Liquids work.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;",
		ordinal = 0),
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;")
	private HitResult liquidsRaycast(Entity instance, double maxDistance,
		float tickDelta, boolean includeFluids, Operation<HitResult> original)
	{
		if(!WurstClient.INSTANCE.getHax().liquidsHack.isEnabled())
			return original.call(instance, maxDistance, tickDelta,
				includeFluids);
		
		return original.call(instance, maxDistance, tickDelta, true);
	}
	
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
			ordinal = 0),
		method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
	private float onRenderWorldNauseaLerp(float delta, float start, float end,
		Operation<Float> original)
	{
		if(!WurstClient.INSTANCE.getHax().antiWobbleHack.isEnabled())
			return original.call(delta, start, end);
		
		return 0;
	}
	
	@Inject(at = @At("HEAD"),
		method = "getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F",
		cancellable = true)
	private static void onGetNightVisionStrength(LivingEntity entity,
		float tickDelta, CallbackInfoReturnable<Float> cir)
	{
		FullbrightHack fullbright =
			WurstClient.INSTANCE.getHax().fullbrightHack;
		
		if(fullbright.isNightVisionActive())
			cir.setReturnValue(fullbright.getNightVisionStrength());
	}
	
	@Inject(at = @At("HEAD"),
		method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
		cancellable = true)
	private void onTiltViewWhenHurt(PoseStack matrices, float tickDelta,
		CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noHurtcamHack.isEnabled())
			ci.cancel();
	}
}
