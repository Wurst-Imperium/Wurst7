/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.HackList;

@Mixin(FluidBlock.class)
public abstract class FluidBlockMixin extends Block implements FluidDrainable
{
	private FluidBlockMixin(WurstClient wurst, Settings settings)
	{
		super(settings);
	}
	
	@Inject(method = "getCollisionShape",
		at = @At(value = "HEAD"),
		cancellable = true)
	private void getCollisionShape(BlockState state, BlockView world,
		BlockPos pos, ShapeContext context,
		CallbackInfoReturnable<VoxelShape> cir)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		
		if(hax != null && hax.jesusHack.shouldBeSolid())
		{
			cir.setReturnValue(VoxelShapes.fullCube());
			cir.cancel();
		}
	}
}
