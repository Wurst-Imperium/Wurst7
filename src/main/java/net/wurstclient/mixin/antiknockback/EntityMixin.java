/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiknockback;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(Entity.class)
public abstract class EntityMixin
{
	/**
	 * Applies AntiKnockback's strength settings to regular knockback for the
	 * local player.
	 */
	@ModifyVariable(method = "lerpMotion(Lnet/minecraft/world/phys/Vec3;)V",
		at = @At("HEAD"),
		argsOnly = true)
	private Vec3 modifyLerpMotion(Vec3 original)
	{
		if((Object)this != WurstClient.MC.player)
			return original;
		
		return WurstClient.INSTANCE.getHax().antiKnockbackHack
			.modifyKnockback(original);
	}
}
