/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.WurstClient;

@Mixin(ClientLevel.class)
public class ClientWorldMixin
{
	@Shadow
	@Final
	private Minecraft minecraft;
	
	/**
	 * This is the part that makes BarrierESP work.
	 */
	@Inject(at = @At("HEAD"),
		method = "getMarkerParticleTarget()Lnet/minecraft/world/level/block/Block;",
		cancellable = true)
	private void onGetBlockParticle(CallbackInfoReturnable<Block> cir)
	{
		if(!WurstClient.INSTANCE.getHax().barrierEspHack.isEnabled())
			return;
			
		// Pause BarrierESP when holding a light in Creative Mode, since it
		// would otherwise prevent the player from seeing light blocks.
		if(minecraft.gameMode.getPlayerMode() == GameType.CREATIVE
			&& minecraft.player.getMainHandItem().getItem() == Items.LIGHT)
			return;
		
		cir.setReturnValue(Blocks.BARRIER);
	}
}
