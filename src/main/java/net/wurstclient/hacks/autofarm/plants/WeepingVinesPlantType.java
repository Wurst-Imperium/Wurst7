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

public final class WeepingVinesPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return (state.is(Blocks.WEEPING_VINES)
			|| state.is(Blocks.WEEPING_VINES_PLANT)) && hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockState ceiling = BlockUtils.getState(pos.above());
		return !ceiling.is(Blocks.WEEPING_VINES)
			&& !ceiling.is(Blocks.WEEPING_VINES_PLANT)
			&& ceiling.isFaceSturdy(WurstClient.MC.level, pos, Direction.DOWN);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.WEEPING_VINES;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		return (state.is(Blocks.WEEPING_VINES)
			|| state.is(Blocks.WEEPING_VINES_PLANT))
			&& !isReplantingSpot(pos, state);
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Weeping Vines", Items.WEEPING_VINES, false,
			false);
	}
}
