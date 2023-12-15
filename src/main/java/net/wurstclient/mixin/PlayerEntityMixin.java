/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
		opcode = Opcodes.INVOKEVIRTUAL,
		ordinal = 0), method = "attack(Lnet/minecraft/entity/Entity;)V")
	private void setVelocityOfAttacker(PlayerEntity entity, Vec3d velocity)
	{
		if(!WurstClient.INSTANCE.getHax().autoSprintHack.shouldSprintAttack())
			entity.setVelocity(velocity);
	}
	
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V",
		opcode = Opcodes.INVOKEVIRTUAL,
		ordinal = 0), method = "attack(Lnet/minecraft/entity/Entity;)V")
	private void setAttackerSprinting(PlayerEntity entity, boolean sprinting)
	{
		if(!WurstClient.INSTANCE.getHax().autoSprintHack.shouldSprintAttack())
			entity.setSprinting(sprinting);
	}
}
