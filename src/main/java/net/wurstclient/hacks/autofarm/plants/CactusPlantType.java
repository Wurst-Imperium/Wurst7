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
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class CactusPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		if(!state.isOf(Blocks.CACTUS))
			return false;
		
		return !BlockUtils.getState(pos.down()).isOf(Blocks.CACTUS)
			&& hasPlantingSurface(pos);
	}
	
	// isSolid() is deprecated but still used by CactusBlock
	@SuppressWarnings("deprecation")
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockPos floorPos = pos.down();
		BlockState floor = BlockUtils.getState(floorPos);
		if(!floor.isIn(BlockTags.SAND))
			return false;
		
		return Direction.Type.HORIZONTAL.stream().map(pos::offset)
			.map(BlockUtils::getState).noneMatch(neighbor -> neighbor.isSolid()
				|| neighbor.getFluidState().isIn(FluidTags.LAVA));
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.CACTUS;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(!state.isOf(Blocks.CACTUS) && !state.isOf(Blocks.CACTUS_FLOWER))
			return false;
		
		BlockPos below = pos.down();
		return isReplantingSpot(below, BlockUtils.getState(below));
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Cactus", Items.CACTUS, true, true);
	}
}
