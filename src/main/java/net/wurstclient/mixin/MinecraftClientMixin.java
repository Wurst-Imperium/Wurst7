/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.blaze3d.platform.WindowEventHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.HandleBlockBreakingListener.HandleBlockBreakingEvent;
import net.wurstclient.events.HandleInputListener.HandleInputEvent;
import net.wurstclient.events.LeftClickListener.LeftClickEvent;
import net.wurstclient.events.RightClickListener.RightClickEvent;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin
	extends ReentrantBlockableEventLoop<Runnable>
	implements WindowEventHandler, IMinecraftClient
{
	@Shadow
	@Final
	public File gameDirectory;
	@Shadow
	public MultiPlayerGameMode gameMode;
	@Shadow
	public LocalPlayer player;
	
	@Unique
	private YggdrasilAuthenticationService wurstAuthenticationService;
	
	private User wurstSession;
	private ProfileKeyPairManager wurstProfileKeys;
	
	private MinecraftClientMixin(WurstClient wurst, String name)
	{
		super(name);
	}
	
	@Inject(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/Services;create(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Ljava/io/File;)Lnet/minecraft/server/Services;",
		shift = At.Shift.AFTER), method = "<init>")
	private void captureAuthenticationService(GameConfig args, CallbackInfo ci,
		@Local YggdrasilAuthenticationService yggdrasilAuthenticationService)
	{
		wurstAuthenticationService = yggdrasilAuthenticationService;
	}
	
	/**
	 * Runs just before {@link Minecraft#handleKeybinds()}, bypassing
	 * the <code>overlay == null && currentScreen == null</code> check in
	 * {@link Minecraft#tick()}.
	 */
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/Minecraft;overlay:Lnet/minecraft/client/gui/screens/Overlay;",
		ordinal = 0), method = "tick()V")
	private void onHandleInputEvents(CallbackInfo ci)
	{
		// Make sure this event is not fired outside of gameplay
		if(player == null)
			return;
		
		EventManager.fire(HandleInputEvent.INSTANCE);
	}
	
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
		ordinal = 0), method = "startAttack()Z", cancellable = true)
	private void onDoAttack(CallbackInfoReturnable<Boolean> cir)
	{
		LeftClickEvent event = new LeftClickEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			cir.setReturnValue(false);
	}
	
	@Inject(
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I",
			ordinal = 0),
		method = "startUseItem()V",
		cancellable = true)
	private void onDoItemUse(CallbackInfo ci)
	{
		RightClickEvent event = new RightClickEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = @At("HEAD"), method = "pickBlock()V")
	private void onDoItemPick(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		HitResult hitResult = WurstClient.MC.hitResult;
		if(!(hitResult instanceof EntityHitResult eHitResult))
			return;
		
		WurstClient.INSTANCE.getFriends().middleClick(eHitResult.getEntity());
	}
	
	/**
	 * Allows hacks to cancel vanilla block breaking and replace it with their
	 * own. Useful for Nuker-like hacks.
	 */
	@Inject(at = @At("HEAD"), method = "continueAttack(Z)V", cancellable = true)
	private void onHandleBlockBreaking(boolean breaking, CallbackInfo ci)
	{
		HandleBlockBreakingEvent event = new HandleBlockBreakingEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = @At("HEAD"),
		method = "getUser()Lnet/minecraft/client/User;",
		cancellable = true)
	private void onGetSession(CallbackInfoReturnable<User> cir)
	{
		if(wurstSession != null)
			cir.setReturnValue(wurstSession);
	}
	
	@Inject(at = @At("RETURN"),
		method = "getGameProfile()Lcom/mojang/authlib/GameProfile;",
		cancellable = true)
	public void onGetGameProfile(CallbackInfoReturnable<GameProfile> cir)
	{
		if(wurstSession == null)
			return;
		
		GameProfile oldProfile = cir.getReturnValue();
		GameProfile newProfile = new GameProfile(wurstSession.getProfileId(),
			wurstSession.getName(), oldProfile.properties());
		cir.setReturnValue(newProfile);
	}
	
	@Inject(at = @At("HEAD"),
		method = "getProfileKeyPairManager()Lnet/minecraft/client/multiplayer/ProfileKeyPairManager;",
		cancellable = true)
	private void onGetProfileKeys(
		CallbackInfoReturnable<ProfileKeyPairManager> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
		
		if(wurstProfileKeys == null)
			return;
		
		cir.setReturnValue(wurstProfileKeys);
	}
	
	@Inject(at = @At("HEAD"), method = "allowsTelemetry()Z", cancellable = true)
	private void onIsTelemetryEnabledByApi(CallbackInfoReturnable<Boolean> cir)
	{
		cir.setReturnValue(
			!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled());
	}
	
	@Inject(at = @At("HEAD"),
		method = "extraTelemetryAvailable()Z",
		cancellable = true)
	private void onIsOptionalTelemetryEnabledByApi(
		CallbackInfoReturnable<Boolean> cir)
	{
		cir.setReturnValue(
			!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled());
	}
	
	@Override
	public IClientPlayerEntity getPlayer()
	{
		return (IClientPlayerEntity)player;
	}
	
	@Override
	public IClientPlayerInteractionManager getInteractionManager()
	{
		return (IClientPlayerInteractionManager)gameMode;
	}
	
	@Override
	public User getWurstSession()
	{
		return wurstSession;
	}
	
	@Override
	public void setWurstSession(User session)
	{
		wurstSession = session;
		if(session == null)
		{
			wurstProfileKeys = null;
			return;
		}
		
		String accessToken = session.getAccessToken();
		boolean isOffline = accessToken == null || accessToken.isBlank()
			|| accessToken.equals("0") || accessToken.equals("null");
		UserApiService userApiService = isOffline ? UserApiService.OFFLINE
			: wurstAuthenticationService.createUserApiService(accessToken);
		wurstProfileKeys = ProfileKeyPairManager.create(userApiService, session,
			gameDirectory.toPath());
	}
}
