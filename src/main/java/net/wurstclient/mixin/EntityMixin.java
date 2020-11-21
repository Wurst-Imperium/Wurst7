/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.VelocityFromFluidListener.VelocityFromFluidEvent;

@Mixin(Entity.class)
public abstract class EntityMixin implements Nameable, CommandOutput
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
		opcode = Opcodes.INVOKEVIRTUAL,
		ordinal = 0),
		method = {"updateMovementInFluid(Lnet/minecraft/tag/Tag;D)Z"})
	private void setVelocityFromFluid(Entity entity, Vec3d velocity)
	{
		VelocityFromFluidEvent event = new VelocityFromFluidEvent();
		EventManager.fire(event);
		
		if(!event.isCancelled())
			entity.setVelocity(velocity);
	}
}
