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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.wurstclient.WurstClient;

@Mixin(ClientWorld.class)
public class ClientWorldMixin
{
	/**
	 * This is the part that makes BarrierESP work.
	 */
	@Inject(at = @At("HEAD"),
		method = "getBlockParticle()Lnet/minecraft/block/Block;",
		cancellable = true)
	private void getBlockParticle(CallbackInfoReturnable<Block> cir)
	{
		if(WurstClient.INSTANCE.getHax().barrierEspHack.isEnabled())
			cir.setReturnValue(Blocks.BARRIER);
	}
}
