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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(TridentItem.class)
public class TridentItemMixin
{
	/**
	 * allows riptide to work out of water or rain when Out-of-water is enabled.
	 */
	@Redirect(
		method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
	private boolean redirectIsInWaterOrRainUse(Player player)
	{
		
		if(WurstClient.INSTANCE.getHax().tridentBoostHack.isOutOfWaterAllowed())
			return true;
		
		return player.isInWaterOrRain();
	}
	
	@Redirect(
		method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
	private boolean redirectIsInWaterOrRainRelease(Player player)
	{
		
		if(WurstClient.INSTANCE.getHax().tridentBoostHack.isOutOfWaterAllowed())
			return true;
		
		return player.isInWaterOrRain();
	}
	
	/**
	 * multiplies the player's velocity after launch.
	 */
	@Inject(
		method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V",
			shift = At.Shift.AFTER))
	private void onAfterRiptidePush(ItemStack stack, Level level,
		LivingEntity entity, int remainingTime,
		CallbackInfoReturnable<Boolean> cir)
	{
		double mult =
			WurstClient.INSTANCE.getHax().tridentBoostHack.getMultiplier();
		
		if(mult == 1)
			return;
		
		Vec3 v = entity.getDeltaMovement();
		entity.setDeltaMovement(v.x * mult, v.y * mult, v.z * mult);
	}
}
