/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;

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
	private FluidBlockMixin(WurstClient wurst, Settings block$Settings_1)
	{
		super(block$Settings_1);
	}
	
	@Override
	public VoxelShape getCollisionShape(BlockState blockState_1,
		BlockView blockView_1, BlockPos blockPos_1,
		ShapeContext entityContext_1)
	{
		HackList hax = WurstClient.INSTANCE.getHax();
		if(hax != null && hax.jesusHack.shouldBeSolid())
			return VoxelShapes.fullCube();
		
		return super.getCollisionShape(blockState_1, blockView_1, blockPos_1,
			entityContext_1);
	}
}
