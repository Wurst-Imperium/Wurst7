/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm.plants;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class PumpkinPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return state.is(Blocks.PUMPKIN_STEM)
			|| state.is(Blocks.ATTACHED_PUMPKIN_STEM);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		return BlockUtils.getState(pos.below()).is(Blocks.FARMLAND);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.PUMPKIN_SEEDS;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		return state.is(Blocks.PUMPKIN);
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Pumpkins", Items.PUMPKIN, true, true);
	}
}
