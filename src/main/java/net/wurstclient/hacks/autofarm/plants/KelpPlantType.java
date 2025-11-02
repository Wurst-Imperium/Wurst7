/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm.plants;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class KelpPlantType extends AutoFarmPlantType
{
	// KELP is the tip, KELP_PLANT is any other part below that.
	// Either one can be the stem, depending on the size of the plant.
	
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		if(!state.isOf(Blocks.KELP) && !state.isOf(Blocks.KELP_PLANT))
			return false;
		
		BlockState floor = BlockUtils.getState(pos.down());
		return !floor.isOf(Blocks.KELP) && !floor.isOf(Blocks.KELP_PLANT)
			&& hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		FluidState fluid = BlockUtils.getState(pos).getFluidState();
		if(!fluid.isIn(FluidTags.WATER) || fluid.getLevel() != 8)
			return false;
		
		BlockState floor = BlockUtils.getState(pos.down());
		return !floor.isOf(Blocks.MAGMA_BLOCK) && floor
			.isSideSolidFullSquare(WurstClient.MC.world, pos, Direction.UP);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.KELP;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(!state.isOf(Blocks.KELP) && !state.isOf(Blocks.KELP_PLANT))
			return false;
		
		BlockPos below = pos.down();
		return isReplantingSpot(below, BlockUtils.getState(below));
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Kelp", Items.KELP, true, true);
	}
}
