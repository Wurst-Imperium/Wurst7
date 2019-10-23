/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.NonBlockingThreadExecutor;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.snooper.Snooper;
import net.minecraft.util.snooper.SnooperListener;
import net.wurstclient.WurstClient;
import net.wurstclient.events.LeftClickListener.LeftClickEvent;
import net.wurstclient.mixinterface.IClientPlayerEntity;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin extends NonBlockingThreadExecutor<Runnable>
	implements SnooperListener, WindowEventHandler, AutoCloseable,
	IMinecraftClient
{
	@Shadow
	private int itemUseCooldown;
	@Shadow
	private ClientPlayerInteractionManager interactionManager;
	@Shadow
	private ClientPlayerEntity player;
	
	private MinecraftClientMixin(WurstClient wurst, String string_1)
	{
		super(string_1);
	}
	
	@Inject(at = {@At(value = "FIELD",
		target = "Lnet/minecraft/client/MinecraftClient;hitResult:Lnet/minecraft/util/hit/HitResult;",
		ordinal = 0)}, method = {"doAttack()V"}, cancellable = true)
	private void onDoAttack(CallbackInfo ci)
	{
		LeftClickEvent event = new LeftClickEvent();
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}
	
	@Inject(at = {@At("HEAD")}, method = {"doItemPick()V"})
	private void onDoItemPick(CallbackInfo ci)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		HitResult hitResult = WurstClient.MC.hitResult;
		if(hitResult == null || hitResult.getType() != HitResult.Type.ENTITY)
			return;
		
		Entity entity = ((EntityHitResult)hitResult).getEntity();
		WurstClient.INSTANCE.getFriends().middleClick(entity);
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
	
	@Shadow
	private void doItemUse()
	{
		
	}
	
	@Override
	public void send(Runnable var1)
	{
		throw new RuntimeException();
	}
	
	@Shadow
	@Override
	public void close()
	{
		
	}
	
	@Shadow
	@Override
	public void onWindowFocusChanged(boolean var1)
	{
		
	}
	
	@Shadow
	@Override
	public void updateDisplay(boolean var1)
	{
		
	}
	
	@Shadow
	@Override
	public void onResolutionChanged()
	{
		
	}
	
	@Shadow
	@Override
	public void addSnooperInfo(Snooper var1)
	{
		
	}
	
	@Shadow
	@Override
	protected Runnable prepareRunnable(Runnable var1)
	{
		return null;
	}
	
	@Shadow
	@Override
	protected boolean canRun(Runnable var1)
	{
		return false;
	}
	
	@Shadow
	@Override
	protected Thread getThread()
	{
		return null;
	}
}
