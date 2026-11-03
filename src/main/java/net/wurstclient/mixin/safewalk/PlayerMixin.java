/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.safewalk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(Player.class)
public abstract class PlayerMixin
{
	/**
	 * Prevents the local player from walking off ledges if SafeWalk is enabled.
	 */
	@ModifyReturnValue(method = "isStayingOnGroundSurface()Z",
		at = @At("RETURN"))
	private boolean modifyIsStayingOnGroundSurface(boolean original)
	{
		if((Object)this != WurstClient.MC.player)
			return original;
		
		return original
			|| WurstClient.INSTANCE.getHax().safeWalkHack.isEnabled();
	}
	
	/**
	 * Allows SafeWalk to make the local player sneak visibly when near a ledge.
	 */
	@Inject(
		method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
		at = @At("RETURN"))
	private void modifyMaybeBackOffFromEdge(Vec3 movement, MoverType type,
		CallbackInfoReturnable<Vec3> cir)
	{
		if((Object)this != WurstClient.MC.player || movement == null)
			return;
		
		WurstClient.INSTANCE.getHax().safeWalkHack
			.onMaybeBackOffFromEdge(!movement.equals(cir.getReturnValue()));
	}
}
