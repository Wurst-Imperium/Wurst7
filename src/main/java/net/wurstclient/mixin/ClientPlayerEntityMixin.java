/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.AirStrafingSpeedListener.AirStrafingSpeedEvent;
import net.wurstclient.events.IsPlayerInLavaListener.IsPlayerInLavaEvent;
import net.wurstclient.events.IsPlayerInWaterListener.IsPlayerInWaterEvent;
import net.wurstclient.events.KnockbackListener.KnockbackEvent;
import net.wurstclient.events.PlayerMoveListener.PlayerMoveEvent;
import net.wurstclient.events.PostMotionListener.PostMotionEvent;
import net.wurstclient.events.PreMotionListener.PreMotionEvent;
import net.wurstclient.events.UpdateListener.UpdateEvent;
import net.wurstclient.hack.HackList;
import net.wurstclient.mixinterface.IClientPlayerEntity;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
	implements IClientPlayerEntity
{
	@Shadow
	@Final
	protected MinecraftClient client;
	
	private Screen tempCurrentScreen;
	
	public ClientPlayerEntityMixin(WurstClient wurst, ClientWorld world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
		ordinal = 0), method = "tick()V")
	private void onTick(CallbackInfo ci)
	{
		EventManager.fire(UpdateEvent.INSTANCE);
	}
	
	/**
	 * This mixin makes AutoSprint's "Omnidirectional Sprint" setting work.
	 */
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/input/Input;hasForwardMovement()Z",
			ordinal = 0),
		method = "tickMovement()V")
	private boolean wrapHasForwardMovement(Input input,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().autoSprintHack.shouldOmniSprint())
			return input.getMovementInput().length() > 1e-5F;
		
		return original.call(input);
	}
	
	/**
	 * Allows NoSlowdown to intercept the isUsingItem() call in
	 * tickMovement().
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
		ordinal = 0), method = "tickMovement()V")
	private boolean wrapTickMovementItemUse(ClientPlayerEntity instance,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			return false;
		
		return original.call(instance);
	}
	
	@Inject(at = @At("HEAD"), method = "sendMovementPackets()V")
	private void onSendMovementPacketsHEAD(CallbackInfo ci)
	{
		EventManager.fire(PreMotionEvent.INSTANCE);
	}
	
	@Inject(at = @At("TAIL"), method = "sendMovementPackets()V")
	private void onSendMovementPacketsTAIL(CallbackInfo ci)
	{
		EventManager.fire(PostMotionEvent.INSTANCE);
	}
	
	@Inject(at = @At("HEAD"),
		method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V")
	private void onMove(MovementType type, Vec3d offset, CallbackInfo ci)
	{
		EventManager.fire(PlayerMoveEvent.INSTANCE);
	}
	
	@Inject(at = @At("HEAD"),
		method = "isAutoJumpEnabled()Z",
		cancellable = true)
	private void onIsAutoJumpEnabled(CallbackInfoReturnable<Boolean> cir)
	{
		if(!WurstClient.INSTANCE.getHax().stepHack.isAutoJumpAllowed())
			cir.setReturnValue(false);
	}
	
	/**
	 * When PortalGUI is enabled, this mixin temporarily sets the current screen
	 * to null to prevent the updateNausea() method from closing it.
	 */
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;",
		opcode = Opcodes.GETFIELD,
		ordinal = 0), method = "tickNausea(Z)V")
	private void beforeTickNausea(boolean fromPortalEffect, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().portalGuiHack.isEnabled())
			return;
		
		tempCurrentScreen = client.currentScreen;
		client.currentScreen = null;
	}
	
	/**
	 * This mixin restores the current screen as soon as the updateNausea()
	 * method is done looking at it.
	 */
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/network/ClientPlayerEntity;nauseaIntensity:F",
		opcode = Opcodes.GETFIELD,
		ordinal = 1), method = "tickNausea(Z)V")
	private void afterTickNausea(boolean fromPortalEffect, CallbackInfo ci)
	{
		if(tempCurrentScreen == null)
			return;
		
		client.currentScreen = tempCurrentScreen;
		tempCurrentScreen = null;
	}
	
	/**
	 * This mixin allows AutoSprint to enable sprinting even when the player is
	 * too hungry.
	 */
	@Inject(at = @At("HEAD"), method = "canSprint()Z", cancellable = true)
	private void onCanSprint(CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().autoSprintHack.shouldSprintHungry())
			cir.setReturnValue(true);
	}
	
	/**
	 * Getter method for what used to be airStrafingSpeed.
	 * Overridden to allow for the speed to be modified by hacks.
	 */
	@Override
	protected float getOffGroundSpeed()
	{
		AirStrafingSpeedEvent event =
			new AirStrafingSpeedEvent(super.getOffGroundSpeed());
		EventManager.fire(event);
		return event.getSpeed();
	}
	
	@Override
	public void setVelocityClient(Vec3d vec)
	{
		KnockbackEvent event = new KnockbackEvent(vec.x, vec.y, vec.z);
		EventManager.fire(event);
		super.setVelocityClient(
			new Vec3d(event.getX(), event.getY(), event.getZ()));
	}
	
	@Override
	public boolean isTouchingWater()
	{
		boolean inWater = super.isTouchingWater();
		IsPlayerInWaterEvent event = new IsPlayerInWaterEvent(inWater);
		EventManager.fire(event);
		
		return event.isInWater();
	}
	
	@Override
	public boolean isInLava()
	{
		boolean inLava = super.isInLava();
		IsPlayerInLavaEvent event = new IsPlayerInLavaEvent(inLava);
		EventManager.fire(event);
		
		return event.isInLava();
	}
	
	@Override
	public boolean isSpectator()
	{
		return super.isSpectator()
			|| WurstClient.INSTANCE.getHax().freecamHack.isEnabled();
	}
	
	@Override
	public boolean isTouchingWaterBypass()
	{
		return super.isTouchingWater();
	}
	
	@Override
	protected float getJumpVelocity()
	{
		return super.getJumpVelocity()
			+ WurstClient.INSTANCE.getHax().highJumpHack
				.getAdditionalJumpMotion();
	}
	
	/**
	 * This is the part that makes SafeWalk work.
	 */
	@Override
	protected boolean clipAtLedge()
	{
		return super.clipAtLedge()
			|| WurstClient.INSTANCE.getHax().safeWalkHack.isEnabled();
	}
	
	/**
	 * This mixin allows SafeWalk to sneak visibly when the player is
	 * near a ledge.
	 */
	@Override
	protected Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type)
	{
		Vec3d result = super.adjustMovementForSneaking(movement, type);
		
		if(movement != null)
			WurstClient.INSTANCE.getHax().safeWalkHack
				.onClipAtLedge(!movement.equals(result));
		
		return result;
	}
	
	@Override
	public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		
		if(effect == StatusEffects.NIGHT_VISION
			&& hax.fullbrightHack.isNightVisionActive())
			return true;
		
		if(effect == StatusEffects.LEVITATION
			&& hax.noLevitationHack.isEnabled())
			return false;
		
		if(effect == StatusEffects.BLINDNESS && hax.antiBlindHack.isEnabled())
			return false;
		
		if(effect == StatusEffects.DARKNESS && hax.antiBlindHack.isEnabled())
			return false;
		
		return super.hasStatusEffect(effect);
	}
	
	@Override
	public StatusEffectInstance getStatusEffect(
		RegistryEntry<StatusEffect> effect)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		
		if(effect == StatusEffects.LEVITATION
			&& hax.noLevitationHack.isEnabled())
			return null;
		
		return super.getStatusEffect(effect);
	}
	
	@Override
	public float getStepHeight()
	{
		return WurstClient.INSTANCE.getHax().stepHack
			.adjustStepHeight(super.getStepHeight());
	}
	
	@Override
	public double getBlockInteractionRange()
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return super.getBlockInteractionRange();
		
		return hax.reachHack.getReachDistance();
	}
	
	@Override
	public double getEntityInteractionRange()
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return super.getEntityInteractionRange();
		
		return hax.reachHack.getReachDistance();
	}
}
