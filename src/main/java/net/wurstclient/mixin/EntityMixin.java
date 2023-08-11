/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.util.ColorUtils;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.VelocityFromEntityCollisionListener.VelocityFromEntityCollisionEvent;
import net.wurstclient.events.VelocityFromFluidListener.VelocityFromFluidEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements Nameable, CommandOutput
{
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/Entity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V",
		opcode = Opcodes.INVOKEVIRTUAL,
		ordinal = 0),
		method = "updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z")
	private void setVelocityFromFluid(Entity entity, Vec3d velocity)
	{
		VelocityFromFluidEvent event =
			new VelocityFromFluidEvent((Entity)(Object)this);
		EventManager.fire(event);
		
		if(!event.isCancelled())
			entity.setVelocity(velocity);
	}
	
	@Inject(at = @At("HEAD"),
		method = "Lnet/minecraft/entity/Entity;pushAwayFrom(Lnet/minecraft/entity/Entity;)V",
		cancellable = true)
	private void onPushAwayFrom(Entity entity, CallbackInfo ci)
	{
		VelocityFromEntityCollisionEvent event =
			new VelocityFromEntityCollisionEvent((Entity)(Object)this);
		EventManager.fire(event);
		
		if(event.isCancelled())
			ci.cancel();
	}

	@Inject(at = @At("HEAD"),
			method = "getDisplayName()Lnet/minecraft/text/Text;",
			cancellable = true)
	private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
		//noinspection ConstantValue
		if ((Object)this instanceof ItemEntity
				&& WurstClient.INSTANCE.getHax().itemEspHack.shouldRenderItemInfo())
		{
			ItemEntity itemEntity = (ItemEntity) ((Object)this);
			int itemAgeRemaining = (6000 - itemEntity.getItemAge()) / 20;

			// There is a problem with this
			//	TimerHack (or any other client-side delta manipulation) interferes
			//	Single player world instances cause item ages to be inaccurate to
			//	the locally started server instance
			// Single Player worlds start an internal server (MC.server) that is separate from the client MC.world
			//	In the serverworld, the item age works as expected, however, item age (with TimerHack enabled)
			//		is not accurate

			// 1.19 hex-colors could easily be used
			//	like a gradient from green->yellow->orange->red

			String text = String.format("\u00a7%s%ds",
					itemAgeRemaining < 15 ? "4" : itemAgeRemaining < 60 ? ("c") : itemAgeRemaining < 180 ? "6" : "2",
					itemAgeRemaining);

			cir.setReturnValue(Text.of(text));

			//cir.setReturnValue(Text.of(String.format("age: %.01fs, itemAge: %.01f", ageSeconds, itemAgeSeconds)));
		}
	}
}
