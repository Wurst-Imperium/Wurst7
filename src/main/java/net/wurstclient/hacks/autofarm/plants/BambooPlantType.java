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
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
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
		if(!state.isOf(Blocks.BAMBOO) && !state.isOf(Blocks.BAMBOO_SAPLING))
			return false;
		
		BlockState floor = BlockUtils.getState(pos.down());
		return !floor.isOf(Blocks.BAMBOO) && !floor.isOf(Blocks.BAMBOO_SAPLING)
			&& floor.isIn(BlockTags.BAMBOO_PLANTABLE_ON);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		return BlockUtils.getState(pos.down())
			.isIn(BlockTags.BAMBOO_PLANTABLE_ON);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.BAMBOO;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(!state.isOf(Blocks.BAMBOO))
			return false;
		
		BlockPos below = pos.down();
		return isReplantingSpot(below, BlockUtils.getState(below));
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Bamboo", Items.BAMBOO, true, true);
	}
}
