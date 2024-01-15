/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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
import net.minecraft.entity.effect.StatusEffects;
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
	private boolean hideNextItemUse;
	
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
	 * This mixin runs just before the tickMovement() method calls
	 * isUsingItem(), so that the onIsUsingItem() mixin knows which
	 * call to intercept.
	 */
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
		ordinal = 0), method = "tickMovement()V")
	private void onTickMovementItemUse(CallbackInfo ci)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			hideNextItemUse = true;
	}
	
	/**
	 * Pretends that the player is not using an item when instructed to do so by
	 * the onTickMovement() mixin.
	 */
	@Inject(at = @At("HEAD"), method = "isUsingItem()Z", cancellable = true)
	private void onIsUsingItem(CallbackInfoReturnable<Boolean> cir)
	{
		if(!hideNextItemUse)
			return;
		
		cir.setReturnValue(false);
		hideNextItemUse = false;
	}
	
	/**
	 * This mixin is injected into a random field access later in the
	 * tickMovement() method to ensure that hideNextItemUse is always reset
	 * after the item use slowdown calculation.
	 */
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/network/ClientPlayerEntity;ticksToNextAutojump:I",
		opcode = Opcodes.GETFIELD,
		ordinal = 0), method = "tickMovement()V")
	private void afterIsUsingItem(CallbackInfo ci)
	{
		hideNextItemUse = false;
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
		ordinal = 0), method = "updateNausea()V")
	private void beforeUpdateNausea(CallbackInfo ci)
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
		ordinal = 1), method = "updateNausea()V")
	private void afterUpdateNausea(CallbackInfo ci)
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
	public void setVelocityClient(double x, double y, double z)
	{
		KnockbackEvent event = new KnockbackEvent(x, y, z);
		EventManager.fire(event);
		super.setVelocityClient(event.getX(), event.getY(), event.getZ());
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
	public boolean hasStatusEffect(StatusEffect effect)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		
		if(effect == StatusEffects.NIGHT_VISION
			&& hax.fullbrightHack.isNightVisionActive())
			return true;
		
		if(effect == StatusEffects.LEVITATION
			&& hax.noLevitationHack.isEnabled())
			return false;
		
		if(effect == StatusEffects.DARKNESS && hax.antiBlindHack.isEnabled())
			return false;
		
		return super.hasStatusEffect(effect);
	}
}
