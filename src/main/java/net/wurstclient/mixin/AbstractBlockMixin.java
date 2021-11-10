/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.wurstclient.WurstClient;

@Mixin(AbstractBlock.class)
public class AbstractBlockMixin
{
	@Inject(at = @At("RETURN"),
		method = "calcBlockBreakingDelta(Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F",
		cancellable = true)
	private void onCalcBlockBreakingDelta(BlockState state, PlayerEntity player,
		BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir)
	{
		if (world instanceof World && ((World)world).isClient)
			cir.setReturnValue(cir.getReturnValueF()
				* WurstClient.INSTANCE.getHax().fastBreakHack.getHardnessModifier());
	}
}
