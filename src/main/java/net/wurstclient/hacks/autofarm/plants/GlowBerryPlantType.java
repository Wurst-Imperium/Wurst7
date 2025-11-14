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
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class GlowBerryPlantType extends AutoFarmPlantType
{
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return state.getBlock() instanceof CaveVines && hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		BlockState ceiling = BlockUtils.getState(pos.above());
		return !(ceiling.getBlock() instanceof CaveVines)
			&& ceiling.isFaceSturdy(WurstClient.MC.level, pos, Direction.DOWN);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.GLOW_BERRIES;
	}
	
	@Override
	public boolean shouldHarvestByInteracting(BlockPos pos, BlockState state)
	{
		// Right-click-harvest the top-most part so we don't have to replant it.
		return state.getBlock() instanceof CaveVines
			&& CaveVines.hasGlowBerries(state) && isReplantingSpot(pos, state);
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		// Left-click-harvest any other part so it can grow more berries.
		return state.getBlock() instanceof CaveVines
			&& !isReplantingSpot(pos, state);
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Glow Berries", Items.GLOW_BERRIES, true,
			true);
	}
}
