/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.antiwaterpush;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;

@Mixin(Entity.class)
public abstract class EntityMixin
{
	/**
	 * Prevents fluid currents from pushing the local player while AntiWaterPush
	 * is enabled. It's set to require 0 because it doesn't work in Forge,
	 * when using Sinytra Connector.
	 */
	@WrapWithCondition(
		method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
			opcode = Opcodes.INVOKEVIRTUAL,
			ordinal = 0),
		require = 0)
	private boolean shouldSetVelocity(Entity instance, Vec3 velocity)
	{
		return instance != WurstClient.MC.player
			|| !WurstClient.INSTANCE.getHax().antiWaterPushHack.isEnabled();
	}
}
