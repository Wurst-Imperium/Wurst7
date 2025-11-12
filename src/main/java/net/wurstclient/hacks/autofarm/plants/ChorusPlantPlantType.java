/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.hacks.autofarm.AutoFarmPlantType;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.BlockUtils;

public final class ChorusPlantPlantType extends AutoFarmPlantType
{
	private static final EnumMap<Direction, BooleanProperty> CHORUS_GROWING_DIRECTIONS =
		Maps.newEnumMap(Map.of(Direction.NORTH, ConnectingBlock.NORTH,
			Direction.SOUTH, ConnectingBlock.SOUTH, Direction.WEST,
			ConnectingBlock.WEST, Direction.EAST, ConnectingBlock.EAST,
			Direction.UP, ConnectingBlock.UP));
	
	@Override
	public final boolean isReplantingSpot(BlockPos pos, BlockState state)
	{
		return (state.isOf(Blocks.CHORUS_FLOWER)
			|| state.isOf(Blocks.CHORUS_PLANT)) && hasPlantingSurface(pos);
	}
	
	@Override
	public final boolean hasPlantingSurface(BlockPos pos)
	{
		return BlockUtils.getState(pos.down()).isOf(Blocks.END_STONE);
	}
	
	@Override
	public Item getSeedItem()
	{
		return Items.CHORUS_FLOWER;
	}
	
	@Override
	public boolean shouldHarvestByMining(BlockPos pos, BlockState state)
	{
		if(state.isOf(Blocks.CHORUS_FLOWER))
			return isFlowerFullyGrown(pos, state);
		
		if(state.isOf(Blocks.CHORUS_PLANT))
			return !hasAttachedFlowers(pos, state, new HashSet<>());
		
		return false;
	}
	
	private boolean isFlowerFullyGrown(BlockPos pos, BlockState state)
	{
		return state.get(ChorusFlowerBlock.AGE, 0) == ChorusFlowerBlock.MAX_AGE
			|| !BlockUtils.getState(pos.up()).isAir();
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
			if(!state.get(entry.getValue(), false))
				continue;
			
			Direction direction = entry.getKey();
			BlockPos neighborPos = pos.offset(direction);
			BlockState neighborState = BlockUtils.getState(neighborPos);
			if(neighborState.isOf(Blocks.CHORUS_FLOWER))
				return true;
			
			if(!neighborState.isOf(Blocks.CHORUS_PLANT))
				continue;
				
			// A horizontally adjacent neighbor that connects down is probably
			// connected to the stem, so we can ignore any flowers that it
			// supports.
			if(direction.getAxis().isHorizontal()
				&& neighborState.get(ConnectingBlock.DOWN, false))
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
