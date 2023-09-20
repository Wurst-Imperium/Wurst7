/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.io.File;
import java.util.UUID;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.util.ProfileKeys;
import net.minecraft.client.util.ProfileKeysImpl;
import net.minecraft.client.util.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.LeftClickListener.LeftClickEvent;
import net.wurstclient.events.RightClickListener.RightClickEvent;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.ILanguageManager;
import net.wurstclient.mixinterface.IMinecraftClient;
import net.wurstclient.mixinterface.IWorld;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin
	extends ReentrantThreadExecutor<Runnable>
	implements WindowEventHandler, IMinecraftClient
{
	@Shadow
	@Final
	public File runDirectory;
	@Shadow
	private int itemUseCooldown;
	@Shadow
	public ClientPlayerInteractionManager interactionManager;
	@Shadow
	@Final
	private LanguageManager languageManager;
	@Shadow
	public ClientPlayerEntity player;
	@Shadow
	public ClientWorld world;
	@Shadow
	@Final
	private Session session;
	@Shadow
	@Final
	private YggdrasilAuthenticationService authenticationService;
	
	private Session wurstSession;
	private ProfileKeysImpl wurstProfileKeys;
	
	private MinecraftClientMixin(WurstClient wurst, String name)
	{
		super(name);
	}
	
	@Inject(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;",
		ordinal = 0), method = "doAttack()Z", cancellable = true)
	private void onDoAttack(CallbackInfoReturnable<Boolean> cir)
	{
		LeftClickEvent event = new LeftClickEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			cir.setReturnValue(false);
	}
	
	@Inject(
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/MinecraftClient;itemUseCooldown:I",
			ordinal = 0),
		method = "doItemUse()V",
		cancellable = true)
	private void onDoItemUse(CallbackInfo ci)
	{
		RightClickEvent event = new RightClickEvent();
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = @At("HEAD"), method = "doItemPick()V")
	private void onDoItemPick(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		HitResult hitResult = WurstClient.MC.crosshairTarget;
		if(!(hitResult instanceof EntityHitResult eHitResult))
			return;
		
		WurstClient.INSTANCE.getFriends().middleClick(eHitResult.getEntity());
	}
	
	@Inject(at = @At("HEAD"),
		method = "getSession()Lnet/minecraft/client/util/Session;",
		cancellable = true)
	private void onGetSession(CallbackInfoReturnable<Session> cir)
	{
		if(wurstSession != null)
			cir.setReturnValue(wurstSession);
	}
	
	@Redirect(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;session:Lnet/minecraft/client/util/Session;",
		opcode = Opcodes.GETFIELD,
		ordinal = 0),
		method = "getSessionProperties()Lcom/mojang/authlib/properties/PropertyMap;")
	private Session getSessionForSessionProperties(MinecraftClient mc)
	{
		return wurstSession != null ? wurstSession : session;
	}
	
	@Inject(at = @At("HEAD"),
		method = "getProfileKeys()Lnet/minecraft/client/util/ProfileKeys;",
		cancellable = true)
	private void onGetProfileKeys(CallbackInfoReturnable<ProfileKeys> cir)
	{
		if(WurstClient.INSTANCE.getOtfs().noChatReportsOtf.isActive())
			cir.setReturnValue(ProfileKeys.MISSING);
		
		if(wurstProfileKeys == null)
			return;
		
		cir.setReturnValue(wurstProfileKeys);
	}
	
	@Inject(at = @At("HEAD"),
		method = "isTelemetryEnabledByApi()Z",
		cancellable = true)
	private void onIsTelemetryEnabledByApi(CallbackInfoReturnable<Boolean> cir)
	{
		cir.setReturnValue(
			!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled());
	}
	
	@Inject(at = @At("HEAD"),
		method = "isOptionalTelemetryEnabledByApi()Z",
		cancellable = true)
	private void onIsOptionalTelemetryEnabledByApi(
		CallbackInfoReturnable<Boolean> cir)
	{
		cir.setReturnValue(
			!WurstClient.INSTANCE.getOtfs().noTelemetryOtf.isEnabled());
	}
	
	@Override
	public void rightClick()
	{
		doItemUse();
	}
	
	@Override
	public int getItemUseCooldown()
	{
		return itemUseCooldown;
	}
	
	@Override
	public void setItemUseCooldown(int itemUseCooldown)
	{
		this.itemUseCooldown = itemUseCooldown;
	}
	
	@Override
	public IClientPlayerEntity getPlayer()
	{
		return (IClientPlayerEntity)player;
	}
	
	@Override
	public IWorld getWorld()
	{
		return (IWorld)world;
	}
	
	@Override
	public IClientPlayerInteractionManager getInteractionManager()
	{
		return (IClientPlayerInteractionManager)interactionManager;
	}
	
	@Override
	public ILanguageManager getLanguageManager()
	{
		return (ILanguageManager)languageManager;
	}
	
	@Override
	public void setSession(Session session)
	{
		wurstSession = session;
		
		UserApiService userApiService =
			wurst_createUserApiService(session.getAccessToken());
		UUID uuid = wurstSession.getProfile().getId();
		wurstProfileKeys =
			new ProfileKeysImpl(userApiService, uuid, runDirectory.toPath());
	}
	
	private UserApiService wurst_createUserApiService(String accessToken)
	{
		try
		{
			return authenticationService.createUserApiService(accessToken);
			
		}catch(AuthenticationException e)
		{
			e.printStackTrace();
			return UserApiService.OFFLINE;
		}
	}
	
	@Shadow
	private void doItemUse()
	{
		
	}
}
