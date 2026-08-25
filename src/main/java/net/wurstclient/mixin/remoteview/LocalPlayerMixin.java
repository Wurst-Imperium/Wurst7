/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.remoteview;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.RemoteViewHack;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin
{
	/**
	 * Keeps the real player controllable while using RemoteView outside of
	 * spectator mode.
	 */
	@Inject(method = "isControlledCamera()Z",
		at = @At("HEAD"),
		cancellable = true)
	private void onIsControlledCamera(CallbackInfoReturnable<Boolean> cir)
	{
		RemoteViewHack remoteView =
			WurstClient.INSTANCE.getHax().remoteViewHack;
		LocalPlayer player = (LocalPlayer)(Object)this;
		if(remoteView.isViewingEntity() && !player.isSpectator())
			cir.setReturnValue(true);
	}
	
	/**
	 * Makes RemoteView's "Interact from" setting work.
	 */
	@ModifyVariable(
		method = "raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;",
		at = @At("HEAD"),
		argsOnly = true)
	private Entity modifyInteractionSource(Entity cameraEntity)
	{
		RemoteViewHack remoteView =
			WurstClient.INSTANCE.getHax().remoteViewHack;
		if(remoteView.isClickingFromPlayer())
			return WurstClient.MC.player;
		
		return cameraEntity;
	}
	
	/**
	 * Prevents self-attacking while using RemoteView with "Interact from"
	 * set to "Camera".
	 */
	@ModifyExpressionValue(method = {
		"raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;",
		"pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;"},
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/world/entity/EntitySelector;CAN_BE_PICKED:Ljava/util/function/Predicate;"))
	private static Predicate<Entity> excludePlayer(Predicate<Entity> original)
	{
		RemoteViewHack remoteView =
			WurstClient.INSTANCE.getHax().remoteViewHack;
		if(remoteView.isViewingEntity() && !remoteView.isClickingFromPlayer())
			return original.and(entity -> entity != WurstClient.MC.player);
		
		return original;
	}
}
