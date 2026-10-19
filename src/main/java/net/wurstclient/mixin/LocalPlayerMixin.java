/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.wurstclient.InputFaker;
import net.wurstclient.InputFaker.TempRealInput;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.IsPlayerInWaterListener.IsPlayerInWaterEvent;
import net.wurstclient.events.MobEffectListener.MobEffectEvent;
import net.wurstclient.events.PostMotionListener.PostMotionEvent;
import net.wurstclient.events.PreMotionListener.PreMotionEvent;
import net.wurstclient.events.UpdateListener.UpdateEvent;
import net.wurstclient.mixinterface.ILocalPlayer;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer
	implements ILocalPlayer
{
	private LocalPlayerMixin(WurstClient wurst, ClientLevel world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(method = "tick()V", at = @At("HEAD"))
	private void onTickHead(CallbackInfo ci)
	{
		InputFaker.swapIfNeeded();
	}
	
	@Inject(method = "tick()V", at = @At("RETURN"))
	private void onTickReturn(CallbackInfo ci)
	{
		InputFaker.restoreIfNeeded();
	}
	
	@Inject(method = "rideTick()V", at = @At("HEAD"))
	private void onRideTickHead(CallbackInfo ci)
	{
		InputFaker.swapIfNeeded();
	}
	
	@Inject(method = "rideTick()V", at = @At("RETURN"))
	private void onRideTickReturn(CallbackInfo ci)
	{
		InputFaker.restoreIfNeeded();
	}
	
	@Inject(method = "tick()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
			ordinal = 0))
	private void onTick(CallbackInfo ci)
	{
		try(TempRealInput _ = new TempRealInput())
		{
			EventManager.fire(UpdateEvent.INSTANCE);
		}
	}
	
	@Inject(method = "sendPosition()V", at = @At("HEAD"))
	private void onSendPositionHEAD(CallbackInfo ci)
	{
		EventManager.fire(PreMotionEvent.INSTANCE);
	}
	
	@Inject(method = "sendPosition()V", at = @At("TAIL"))
	private void onSendPositionTAIL(CallbackInfo ci)
	{
		EventManager.fire(PostMotionEvent.INSTANCE);
	}
	
	/**
	 * Prevents vanilla auto-jump from interfering with Step and .goto.
	 */
	@ModifyExpressionValue(method = "updateAutoJump(FF)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;canAutoJump()Z"))
	private boolean modifyCanAutoJump(boolean original)
	{
		return original && !WurstClient.INSTANCE.getHax().stepHack.isEnabled()
			&& !WurstClient.INSTANCE.getCmds().goToCmd.isActive();
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
	public boolean isInWaterBypass()
	{
		return super.isInWater();
	}
	
	@Override
	public boolean hasEffect(Holder<MobEffect> effect)
	{
		return getEffect(effect) != null;
	}
	
	@Override
	public MobEffectInstance getEffect(Holder<MobEffect> effect)
	{
		MobEffectEvent event =
			new MobEffectEvent(effect, super.getEffect(effect));
		EventManager.fire(event);
		return event.getInstance();
	}
	
	/**
	 * Makes Liquids work. Must take priority over Freecam's raycast wrapper.
	 */
	@ModifyArg(
		method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;",
			ordinal = 0),
		index = 2)
	private static boolean modifyIncludeFluidsForLiquids(boolean includeFluids)
	{
		return includeFluids
			|| WurstClient.INSTANCE.getHax().liquidsHack.isEnabled();
	}
}
