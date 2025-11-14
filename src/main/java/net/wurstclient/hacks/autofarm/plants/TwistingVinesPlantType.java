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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class TwistingVinesPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return (state.is(Blocks.TWISTING_VINES)
			|| state.is(Blocks.TWISTING_VINES_PLANT))
			&& hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockState floor = BlockUtils.getState(pos.below());
		return !floor.is(Blocks.TWISTING_VINES)
			&& !floor.is(Blocks.TWISTING_VINES_PLANT)
			&& floor.isFaceSturdy(WurstClient.MC.level, pos, Direction.UP);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.TWISTING_VINES;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		return (state.is(Blocks.TWISTING_VINES)
			|| state.is(Blocks.TWISTING_VINES_PLANT))
			&& !isReplantingSpot(pos, state);
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Twisting Vines", Items.TWISTING_VINES,
			false, false);
	}
}
