/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;

import net.wurstclient.WurstClient;


@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin
{
	@ModifyArgs(
		 at = @At(value = "INVOKE",
		 target = "Lnet/minecraft/client/world/ClientWorld;randomBlockDisplayTick(IIIILnet/minecraft/util/math/random/Random;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos$Mutable;)V"),
		 method = "doRandomBlockDisplayTicks")		 
	private void doRandomBlockDisplayTicks(Args args) 
	{
		if(!WurstClient.INSTANCE.getHax().barrierEspHack.isEnabled())
			return;

		args.set(5, Blocks.BARRIER);
	}
}
