/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
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

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayer
	implements IClientPlayerEntity
{
	@Shadow
	@Final
	protected Minecraft minecraft;
	
	private Screen tempCurrentScreen;
	
	public ClientPlayerEntityMixin(WurstClient wurst, ClientLevel world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
		ordinal = 0), method = "tick()V")
	private void onTick(CallbackInfo ci)
	{
		EventManager.fire(UpdateEvent.INSTANCE);
	}
	
	/**
	 * This mixin makes AutoSprint's "Omnidirectional Sprint" setting work.
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z",
		ordinal = 0), method = "aiStep()V")
	private boolean wrapHasForwardMovement(ClientInput input,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().autoSprintHack.shouldOmniSprint())
			return input.getMoveVector().length() > 1e-5F;
		
		return original.call(input);
	}
	
	/**
	 * Allows NoSlowdown to intercept the isBlockedFromSprinting() call in
	 * tickMovement().
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z",
		ordinal = 0), method = "aiStep()V")
	private boolean wrapTickMovementItemUse(LocalPlayer instance,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			return false;
		
		return original.call(instance);
	}
	
	@Inject(at = @At("HEAD"), method = "sendPosition()V")
	private void onSendMovementPacketsHEAD(CallbackInfo ci)
	{
		EventManager.fire(PreMotionEvent.INSTANCE);
	}
	
	@Inject(at = @At("TAIL"), method = "sendPosition()V")
	private void onSendMovementPacketsTAIL(CallbackInfo ci)
	{
		EventManager.fire(PostMotionEvent.INSTANCE);
	}
	
	@Inject(at = @At("HEAD"),
		method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V")
	private void onMove(MoverType type, Vec3 offset, CallbackInfo ci)
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
		target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
		opcode = Opcodes.GETFIELD,
		ordinal = 0), method = "handlePortalTransitionEffect(Z)V")
	private void beforeTickNausea(boolean fromPortalEffect, CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.getHax().portalGuiHack.isEnabled())
			return;
		
		tempCurrentScreen = minecraft.screen;
		minecraft.screen = null;
	}
	
	/**
	 * This mixin restores the current screen as soon as the updateNausea()
	 * method is done looking at it.
	 */
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F",
		opcode = Opcodes.GETFIELD,
		ordinal = 1), method = "handlePortalTransitionEffect(Z)V")
	private void afterTickNausea(boolean fromPortalEffect, CallbackInfo ci)
	{
		if(tempCurrentScreen == null)
			return;
		
		minecraft.screen = tempCurrentScreen;
		tempCurrentScreen = null;
	}
	
	/**
	 * This mixin allows AutoSprint to enable sprinting even when the player is
	 * too hungry.
	 */
	@Inject(at = @At("HEAD"),
		method = "isSprintingPossible(Z)Z",
		cancellable = true)
	private void onCanSprint(boolean allowTouchingWater,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(WurstClient.INSTANCE.getHax().autoSprintHack.shouldSprintHungry())
			cir.setReturnValue(true);
	}
	
	/**
	 * Getter method for what used to be airStrafingSpeed.
	 * Overridden to allow for the speed to be modified by hacks.
	 */
	@Override
	protected float getFlyingSpeed()
	{
		AirStrafingSpeedEvent event =
			new AirStrafingSpeedEvent(super.getFlyingSpeed());
		EventManager.fire(event);
		return event.getSpeed();
	}
	
	@Override
	public void lerpMotion(Vec3 vec)
	{
		KnockbackEvent event = new KnockbackEvent(vec.x, vec.y, vec.z);
		EventManager.fire(event);
		super.lerpMotion(new Vec3(event.getX(), event.getY(), event.getZ()));
	}
	
	@Override
	public boolean isInWater()
	{
		boolean inWater = super.isInWater();
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
		return super.isInWater();
	}
	
	@Override
	protected float getJumpPower()
	{
		return super.getJumpPower() + WurstClient.INSTANCE.getHax().highJumpHack
			.getAdditionalJumpMotion();
	}
	
	/**
	 * This is the part that makes SafeWalk work.
	 */
	@Override
	protected boolean isStayingOnGroundSurface()
	{
		return super.isStayingOnGroundSurface()
			|| WurstClient.INSTANCE.getHax().safeWalkHack.isEnabled();
	}
	
	/**
	 * This mixin allows SafeWalk to sneak visibly when the player is
	 * near a ledge.
	 */
	@Override
	protected Vec3 maybeBackOffFromEdge(Vec3 movement, MoverType type)
	{
		Vec3 result = super.maybeBackOffFromEdge(movement, type);
		
		if(movement != null)
			WurstClient.INSTANCE.getHax().safeWalkHack
				.onClipAtLedge(!movement.equals(result));
		
		return result;
	}
	
	@Override
	public boolean hasEffect(Holder<MobEffect> effect)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		
		if(effect == MobEffects.NIGHT_VISION
			&& hax.fullbrightHack.isNightVisionActive())
			return true;
		
		if(effect == MobEffects.LEVITATION && hax.noLevitationHack.isEnabled())
			return false;
		
		if(effect == MobEffects.BLINDNESS && hax.antiBlindHack.isEnabled())
			return false;
		
		if(effect == MobEffects.DARKNESS && hax.antiBlindHack.isEnabled())
			return false;
		
		return super.hasEffect(effect);
	}
	
	@Override
	public MobEffectInstance getEffect(Holder<MobEffect> effect)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		
		if(effect == MobEffects.LEVITATION && hax.noLevitationHack.isEnabled())
			return null;
		
		return super.getEffect(effect);
	}
	
	@Override
	public float maxUpStep()
	{
		return WurstClient.INSTANCE.getHax().stepHack
			.adjustStepHeight(super.maxUpStep());
	}
	
	@Override
	public double blockInteractionRange()
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return super.blockInteractionRange();
		
		return hax.reachHack.getReachDistance();
	}
	
	@Override
	public double entityInteractionRange()
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax == null || !hax.reachHack.isEnabled())
			return super.entityInteractionRange();
		
		return hax.reachHack.getReachDistance();
	}
}
