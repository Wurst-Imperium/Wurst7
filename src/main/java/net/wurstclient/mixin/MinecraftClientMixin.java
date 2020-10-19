/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.GlDebugInfo;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Session;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.snooper.SnooperListener;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.events.LeftClickListener.LeftClickEvent;
import net.wurstclient.events.RightClickListener.RightClickEvent;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;
import net.wurstclient.settings.Setting;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin
	extends ReentrantThreadExecutor<Runnable> implements SnooperListener,
	WindowEventHandler, AutoCloseable, IMinecraftClient
{
	@Shadow
	private int itemUseCooldown;
	@Shadow
	private ClientPlayerInteractionManager interactionManager;
	@Shadow
	private ClientPlayerEntity player;
	@Shadow
	private Session session;
	
	private Session wurstSession;
	
	private MinecraftClientMixin(WurstClient wurst, String string_1)
	{
		super(string_1);
	}
	
	@Inject(at = {@At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;",
		ordinal = 0)}, method = {"doAttack()V"}, cancellable = true)
	private void onDoAttack(CallbackInfo ci)
	{
		LeftClickEvent event = new LeftClickEvent();
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = {@At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;itemUseCooldown:I",
		ordinal = 0)}, method = {"doItemUse()V"}, cancellable = true)
	private void onDoItemUse(CallbackInfo ci)
	{
		RightClickEvent event = new RightClickEvent();
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = {@At("HEAD")}, method = {"doItemPick()V"})
	private void onDoItemPick(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		HitResult hitResult = WurstClient.MC.crosshairTarget;
		if(hitResult == null || hitResult.getType() != HitResult.Type.ENTITY)
			return;
		
		Entity entity = ((EntityHitResult)hitResult).getEntity();
		WurstClient.INSTANCE.getFriends().middleClick(entity);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"getSession()Lnet/minecraft/client/util/Session;"},
		cancellable = true)
	private void onGetSession(CallbackInfoReturnable<Session> cir)
	{
		if(wurstSession == null)
			return;
		
		cir.setReturnValue(wurstSession);
	}
	
	@Redirect(at = @At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;session:Lnet/minecraft/client/util/Session;",
		opcode = Opcodes.GETFIELD,
		ordinal = 0),
		method = {
			"getSessionProperties()Lcom/mojang/authlib/properties/PropertyMap;"})
	private Session getSessionForSessionProperties(MinecraftClient mc)
	{
		if(wurstSession != null)
			return wurstSession;
		else
			return session;
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"addDetailsToCrashReport(Lnet/minecraft/util/crash/CrashReport;)Lnet/minecraft/util/crash/CrashReport;"})
	private void onAddDetailsToCrashReport(CrashReport report,
		CallbackInfoReturnable<CrashReport> cir)
	{
		Sentry.configureScope(scope -> {
			HashMap<String, String> map = new HashMap<>();
			map.put("name", GlDebugInfo.getCpuInfo());
			scope.setContexts("cpu", map);
		});
		
		Sentry.configureScope(scope -> {
			
			HashMap<String, String> map = new HashMap<>();
			
			map.put("name", GlDebugInfo.getRenderer());
			map.put("version", GlDebugInfo.getVersion());
			map.put("vendor_name", GlDebugInfo.getVendor());
			
			Window window = WurstClient.MC.getWindow();
			map.put("framebuffer", window.getFramebufferWidth() + "x"
				+ window.getFramebufferHeight());
			
			scope.setContexts("gpu", map);
		});
		
		Sentry.configureScope(scope -> {
			
			scope.setTag("mc.lang",
				WurstClient.MC.getLanguageManager().getLanguage().getCode());
			
			scope.setTag("mc.font",
				WurstClient.MC.forcesUnicodeFont() ? "unicode" : "default");
			
			Screen cs = WurstClient.MC.currentScreen;
			String screen =
				cs == null ? "none" : cs.getClass().getCanonicalName();
			scope.setTag("mc.screen", screen);
		});
		
		Sentry.configureScope(scope -> {
			
			HashMap<String, Object> map = new HashMap<>();
			
			ArrayList<String> enabledHax = WurstClient.INSTANCE.getHax()
				.getAllHax().stream().filter(Hack::isEnabled).map(Hack::getName)
				.collect(Collectors.toCollection(() -> new ArrayList<>()));
			
			map.put("enabled_hacks", enabledHax);
			
			ArrayList<Feature> features = new ArrayList<>();
			features.addAll(WurstClient.INSTANCE.getHax().getAllHax());
			features.addAll(WurstClient.INSTANCE.getCmds().getAllCmds());
			features.addAll(WurstClient.INSTANCE.getOtfs().getAllOtfs());
			
			HashMap<String, HashMap<String, String>> map2 = new HashMap<>();
			for(Feature feature : features)
			{
				Collection<Setting> settings = feature.getSettings().values();
				if(settings.isEmpty())
					continue;
				
				HashMap<String, String> map3 = new HashMap<>();
				for(Setting setting : settings)
					map3.put(setting.getName(), setting.toJson().toString());
				map2.put(feature.getName(), map3);
			}
			map.put("settings", map2);
			
			scope.setContexts("wurst", map);
		});
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"printCrashReport(Lnet/minecraft/util/crash/CrashReport;)V"})
	private static void onPrintCrashReport(CrashReport report, CallbackInfo ci)
	{
		Sentry.captureException(report.getCause());
	}
	
	@Inject(at = {@At("HEAD")},
		method = {"openScreen(Lnet/minecraft/client/gui/screen/Screen;)V"})
	private void onOpenScreen(@Nullable Screen screen, CallbackInfo ci)
	{
		Breadcrumb breadcrumb = new Breadcrumb();
		breadcrumb.setType("navigation");
		breadcrumb.setCategory("screen.change");
		
		Screen cs = WurstClient.MC.currentScreen;
		String from = cs == null ? "none" : cs.getClass().getCanonicalName();
		breadcrumb.setData("from", from);
		
		String to =
			screen == null ? "none" : screen.getClass().getCanonicalName();
		breadcrumb.setData("to", to);
		
		Sentry.addBreadcrumb(breadcrumb);
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
	public IClientPlayerInteractionManager getInteractionManager()
	{
		return (IClientPlayerInteractionManager)interactionManager;
	}
	
	@Override
	public void setSession(Session session)
	{
		wurstSession = session;
	}
	
	@Shadow
	private void doItemUse()
	{
		
	}
}
