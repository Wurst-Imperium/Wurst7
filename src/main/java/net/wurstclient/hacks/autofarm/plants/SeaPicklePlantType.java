/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class SeaPicklePlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return state.getBlock() instanceof SeaPickleBlock
			&& hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockState floor = BlockUtils.getState(pos.below());
		if(!floor.is(BlockTags.CORAL_BLOCKS))
			return false;
		
		FluidState fluid = BlockUtils.getState(pos).getFluidState();
		return fluid.is(FluidTags.WATER) && fluid.getAmount() == 8;
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.SEA_PICKLE;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(!(state.getBlock() instanceof SeaPickleBlock))
			return false;
		
		return state.getValue(SeaPickleBlock.PICKLES) > 1;
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Sea pickles", Items.SEA_PICKLE, true,
			true);
	}
}
