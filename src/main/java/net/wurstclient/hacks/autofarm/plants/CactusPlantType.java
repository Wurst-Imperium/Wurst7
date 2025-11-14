/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class CactusPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		if(!state.is(Blocks.CACTUS))
			return false;
		
		return !BlockUtils.getState(pos.below()).is(Blocks.CACTUS)
			&& hasPlantingSurface(pos);
	}
	
	// isSolid() is deprecated but still used by CactusBlock
	@SuppressWarnings("deprecation")
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockPos floorPos = pos.below();
		BlockState floor = BlockUtils.getState(floorPos);
		if(!floor.is(BlockTags.SAND))
			return false;
		
		return Direction.Plane.HORIZONTAL.stream().map(pos::relative)
			.map(BlockUtils::getState).noneMatch(neighbor -> neighbor.isSolid()
				|| neighbor.getFluidState().is(FluidTags.LAVA));
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.CACTUS;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(!state.is(Blocks.CACTUS))
			return false;
		
		BlockPos below = pos.below();
		return isReplantingSpot(below, BlockUtils.getState(below));
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Cactus", Items.CACTUS, true, true);
	}
}
