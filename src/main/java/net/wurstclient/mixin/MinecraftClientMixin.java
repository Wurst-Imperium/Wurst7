/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.util.Session;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.snooper.SnooperListener;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.wurstclient.WurstClient;
import net.wurstclient.events.LeftClickListener.LeftClickEvent;
import net.wurstclient.events.RightClickListener.RightClickEvent;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;

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
