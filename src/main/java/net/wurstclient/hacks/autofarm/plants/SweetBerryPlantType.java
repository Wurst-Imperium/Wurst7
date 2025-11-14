/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class SweetBerryPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return state.getBlock() instanceof SweetBerryBushBlock
			&& hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockState floor = BlockUtils.getState(pos.below());
		return floor.is(BlockTags.DIRT) || floor.is(Blocks.FARMLAND);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.SWEET_BERRIES;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		return false;
	}
	
	@Override
	public boolean shouldHarvestByInteracting(BlockPos pos, BlockState state)
	{
		if(!(state.getBlock() instanceof SweetBerryBushBlock))
			return false;
		
		return state.getValue(SweetBerryBushBlock.AGE) > 1;
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Sweet Berries", Items.SWEET_BERRIES, true,
			true);
	}
}
