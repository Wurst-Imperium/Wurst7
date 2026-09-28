/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.noslowdown;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.LocalPlayer;
import net.wurstclient.WurstClient;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin
{
	/**
	 * Makes you keep sprinting when using an item while NoSlowdown is enabled.
	 */
	@WrapOperation(method = "aiStep()V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z",
			ordinal = 0))
	private boolean wrapAiStepItemUse(LocalPlayer instance,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			return false;
		
		return original.call(instance);
	}
	
	/**
	 * Prevents item-use movement slowdown while NoSlowdown is enabled.
	 */
	@WrapOperation(
		method = "modifyInput(Lnet/minecraft/world/phys/Vec2;)Lnet/minecraft/world/phys/Vec2;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
			ordinal = 0))
	private boolean wrapModifyInputItemUse(LocalPlayer instance,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			return false;
		
		return original.call(instance);
	}
	
	/**
	 * Allows sprinting to start while using an item when NoSlowdown is enabled.
	 */
	@WrapOperation(method = "canStartSprinting()Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z",
			ordinal = 0))
	private boolean wrapCanStartSprintingItemUse(LocalPlayer instance,
		Operation<Boolean> original)
	{
		if(WurstClient.INSTANCE.getHax().noSlowdownHack.isEnabled())
			return false;
		
		return original.call(instance);
	}
}
