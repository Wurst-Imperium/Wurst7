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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;

public final class AmethystPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return false;
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		for(Direction dir : Direction.values())
		{
			BlockState state = BlockUtils.getState(pos.offset(dir));
			if(state.isOf(Blocks.BUDDING_AMETHYST))
				return true;
		}
		return false;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		return state.isOf(Blocks.AMETHYST_CLUSTER);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.AMETHYST_SHARD;
	}
	
	@Override
	protected CheckboxSetting createHarvestSetting()
	{
		return new CheckboxSetting("Harvest amethyst", true);
	}
	
	@Override
	protected CheckboxSetting createReplantSetting()
	{
		return null;
	}
}
