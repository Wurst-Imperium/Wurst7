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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class BambooPlantType extends AutoFarmPlantType
{
	// Unlike KELP, BAMBOO_SAPLING is not the tip of the plant.
	// It's just a sapling as the name implies.
	
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		if(!state.is(Blocks.BAMBOO) && !state.is(Blocks.BAMBOO_SAPLING))
			return false;
		
		BlockState floor = BlockUtils.getState(pos.below());
		return !floor.is(Blocks.BAMBOO) && !floor.is(Blocks.BAMBOO_SAPLING)
			&& floor.is(BlockTags.BAMBOO_PLANTABLE_ON);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		return BlockUtils.getState(pos.below())
			.is(BlockTags.BAMBOO_PLANTABLE_ON);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.BAMBOO;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(!state.is(Blocks.BAMBOO))
			return false;
		
		BlockPos below = pos.below();
		return isReplantingSpot(below, BlockUtils.getState(below));
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Bamboo", Items.BAMBOO, true, true);
	}
}
