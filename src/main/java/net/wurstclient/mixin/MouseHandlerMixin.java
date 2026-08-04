/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.MouseButtonPressListener.MouseButtonPressEvent;
import net.wurstclient.events.MouseScrollListener.MouseScrollEvent;
import net.wurstclient.events.MouseUpdateListener.MouseUpdateEvent;
import net.wurstclient.hacks.FreecamHack;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin
{
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;
	
	@Inject(
		method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
		at = @At("HEAD"))
	private void onOnButton(long windowHandle, MouseButtonInfo mouseButtonInfo,
		int action, CallbackInfo ci)
	{
		EventManager
			.fire(new MouseButtonPressEvent(mouseButtonInfo.button(), action));
	}
	
	@Inject(method = "onScroll(JDD)V", at = @At("RETURN"))
	private void onOnScroll(long window, double horizontal, double vertical,
		CallbackInfo ci)
	{
		EventManager.fire(new MouseScrollEvent(vertical));
	}
	
	@WrapOperation(method = "turnPlayer(D)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void wrapTurn(LocalPlayer player, double deltaYaw,
		double deltaPitch, Operation<Void> original)
	{
		FreecamHack freecam = WurstClient.INSTANCE.getHax().freecamHack;
		if(freecam.isMovingCamera())
		{
			freecam.turn(deltaYaw, deltaPitch);
			return;
		}
		
		original.call(player, deltaYaw, deltaPitch);
	}
	
	@Inject(method = "handleAccumulatedMovement()V", at = @At("HEAD"))
	private void onHandleAccumulatedMovement(CallbackInfo ci)
	{
		MouseUpdateEvent event =
			new MouseUpdateEvent(accumulatedDX, accumulatedDY);
		EventManager.fire(event);
		accumulatedDX = event.getDeltaX();
		accumulatedDY = event.getDeltaY();
	}
	
	@WrapWithCondition(method = "onScroll(JDD)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private boolean wrapOnScroll(Inventory inventory, int slot)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		return !wurst.getOtfs().zoomOtf.isControllingScrollEvents()
			&& !wurst.getHax().freecamHack.isControllingScrollEvents()
			&& !wurst.getHax().flightHack.isControllingScrollEvents();
	}
}
