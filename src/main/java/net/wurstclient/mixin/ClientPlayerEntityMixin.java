/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ChatOutputListener.ChatOutputEvent;
import net.wurstclient.events.IsPlayerInWaterListener.IsPlayerInWaterEvent;
import net.wurstclient.events.KnockbackListener.KnockbackEvent;
import net.wurstclient.events.PlayerMoveListener.PlayerMoveEvent;
import net.wurstclient.events.PostMotionListener.PostMotionEvent;
import net.wurstclient.events.PreMotionListener.PreMotionEvent;
import net.wurstclient.events.UpdateListener.UpdateEvent;
import net.wurstclient.hacks.FreecamHack;
import net.wurstclient.hacks.FullbrightHack;
import net.wurstclient.mixinterface.IClientPlayerEntity;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin extends AbstractClientPlayerEntity
	implements IClientPlayerEntity
{
	private FreecamHack freeCam = WurstClient.INSTANCE.getHax().freecamHack;
	@Shadow
	private float lastYaw;
	@Shadow
	private float lastPitch;
	@Shadow
	private ClientPlayNetworkHandler networkHandler;
	
	public ClientPlayerEntityMixin(WurstClient wurst, ClientWorld clientWorld_1,
		GameProfile gameProfile_1)
	{
		super(clientWorld_1, gameProfile_1);
	}
	
	@Inject(at = @At("HEAD"),
		method = "sendChatMessage(Ljava/lang/String;)V",
		cancellable = true)
	private void onSendChatMessage(String message, CallbackInfo ci)
	{
		ChatOutputEvent event = new ChatOutputEvent(message);
		EventManager.fire(event);
		
		if(event.isCancelled())
		{
			ci.cancel();
			return;
		}
		
		if(!event.isModified())
			return;
		
		ChatMessageC2SPacket packet =
			new ChatMessageC2SPacket(event.getMessage());
		networkHandler.sendPacket(packet);
		ci.cancel();
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
		ordinal = 0), method = "tick()V")
	private void onTick(CallbackInfo ci)
	{
		EventManager.fire(UpdateEvent.INSTANCE);
	}

	@Inject(at = @At("INVOKE"), method = "pushOutOfBlocks", cancellable = true)
	private void stopPush(CallbackInfo info)
	{
		if(freeCam.isEnabled())
		{
			info.cancel();
		}
	}
	
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z",
		ordinal = 0), method = "tickMovement()V")
	private boolean wurstIsUsingItem(ClientPlayerEntity player)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			return false;
		
		return player.isUsingItem();
	}
	
	@Inject(at = {@At("HEAD")}, method = {"sendMovementPackets()V"})
	private void onSendMovementPacketsHEAD(CallbackInfo ci)
	{
		EventManager.fire(PreMotionEvent.INSTANCE);
	}
	
	@Inject(at = {@At("TAIL")}, method = {"sendMovementPackets()V"})
	private void onSendMovementPacketsTAIL(CallbackInfo ci)
	{
		EventManager.fire(PostMotionEvent.INSTANCE);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"})
	private void onMove(MovementType type, Vec3d offset, CallbackInfo ci)
	{
		PlayerMoveEvent event = new PlayerMoveEvent(this);
		EventManager.fire(event);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"isAutoJumpEnabled()Z"},
		cancellable = true)
	private void onIsAutoJumpEnabled(CallbackInfoReturnable<Boolean> cir)
	{
		if(!WurstClient.INSTANCE.getHax().stepHack.isAutoJumpAllowed())
			cir.setReturnValue(false);
	}
	
	@Redirect(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/Screen;isPauseScreen()Z",
			ordinal = 0),
		method = {"updateNausea()V"})
	private boolean onUpdateNausea(Screen screen)
	{
		if(WurstClient.INSTANCE.getHax().portalGuiHack.isEnabled())
			return true;
		
		return screen.isPauseScreen();
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
	
	@Override
	protected boolean clipAtLedge()
	{
		return super.clipAtLedge()
			|| WurstClient.INSTANCE.getHax().safeWalkHack.isEnabled();
	}
	
	@Override
	public boolean hasStatusEffect(StatusEffect effect)
	{
		FullbrightHack fullbright =
			WurstClient.INSTANCE.getHax().fullbrightHack;
		
		if(effect == StatusEffects.NIGHT_VISION
			&& fullbright.isNightVisionActive())
			return true;
		
		return super.hasStatusEffect(effect);
	}
	
	@Override
	public void setNoClip(boolean noClip)
	{
		this.noClip = noClip;
	}
	
	@Override
	public float getLastYaw()
	{
		return lastYaw;
	}
	
	@Override
	public float getLastPitch()
	{
		return lastPitch;
	}
	
	@Override
	public void setMovementMultiplier(Vec3d movementMultiplier)
	{
		this.movementMultiplier = movementMultiplier;
	}
}
