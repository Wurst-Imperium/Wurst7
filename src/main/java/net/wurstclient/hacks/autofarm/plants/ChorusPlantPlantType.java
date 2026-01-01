/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm.plants;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class ChorusPlantPlantType extends AutoFarmPlantType
{
	private static final EnumMap<Direction, BooleanProperty> CHORUS_GROWING_DIRECTIONS =
		Maps.newEnumMap(Map.of(Direction.NORTH, PipeBlock.NORTH,
			Direction.SOUTH, PipeBlock.SOUTH, Direction.WEST, PipeBlock.WEST,
			Direction.EAST, PipeBlock.EAST, Direction.UP, PipeBlock.UP));
	
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return (state.is(Blocks.CHORUS_FLOWER) || state.is(Blocks.CHORUS_PLANT))
			&& hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		return BlockUtils.getState(pos.below()).is(Blocks.END_STONE);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.CHORUS_FLOWER;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(state.is(Blocks.CHORUS_FLOWER))
			return isFlowerFullyGrown(pos, state);
		
		if(state.is(Blocks.CHORUS_PLANT))
			return !hasAttachedFlowers(pos, state, new HashSet<>());
		
		return false;
	}
	
	private boolean isFlowerFullyGrown(BlockPos pos, BlockState state)
	{
		return state.getValueOrElse(ChorusFlowerBlock.AGE,
			0) == ChorusFlowerBlock.DEAD_AGE
			|| !BlockUtils.getState(pos.above()).isAir();
	}
	
	private boolean hasAttachedFlowers(BlockPos pos, BlockState state,
		HashSet<BlockPos> visited)
	{
		// If the plant is unreasonably large, just assume there's a flower
		// somewhere so we don't harvest it
		if(visited.size() > 1000)
			return true;
		
		if(!visited.add(pos))
			return false;
		
		for(Entry<Direction, BooleanProperty> entry : CHORUS_GROWING_DIRECTIONS
			.entrySet())
		{
			if(!state.getValueOrElse(entry.getValue(), false))
				continue;
			
			Direction direction = entry.getKey();
			BlockPos neighborPos = pos.relative(direction);
			BlockState neighborState = BlockUtils.getState(neighborPos);
			if(neighborState.is(Blocks.CHORUS_FLOWER))
				return true;
			
			if(!neighborState.is(Blocks.CHORUS_PLANT))
				continue;
				
			// A horizontally adjacent neighbor that connects down is probably
			// connected to the stem, so we can ignore any flowers that it
			// supports.
			if(direction.getAxis().isHorizontal()
				&& neighborState.getValueOrElse(PipeBlock.DOWN, false))
				continue;
			
			if(hasAttachedFlowers(neighborPos, neighborState, visited))
				return true;
		}
		
		return false;
	}
	
	@Override
	protected PlantTypeSetting createSetting()
	{
		return new PlantTypeSetting("Chorus Plants", Items.CHORUS_FLOWER, true,
			true);
	}
}
