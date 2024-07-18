/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import net.minecraft.util.math.BlockPos;
import net.wurstclient.hacks.SearchHack;

/**
 * Converts a {@link HashSet} of block positions into an {@link ArrayList} of
 * vertices that can be used to render those blocks.
 * <p>
 * Used by {@link SearchHack Search} and similar hacks.
 */
public enum BlockVertexCompiler
{
	;
	
	public static ArrayList<int[]> compile(HashSet<BlockPos> blocks)
	{
		return blocks.parallelStream().flatMap(pos -> getVertices(pos, blocks))
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public static ArrayList<int[]> compile(HashSet<BlockPos> blocks,
		RegionPos region)
	{
		return blocks.parallelStream().flatMap(pos -> getVertices(pos, blocks))
			.map(v -> applyRegionOffset(v, region))
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	private static int[] applyRegionOffset(int[] vertex, RegionPos region)
	{
		vertex[0] -= region.x();
		vertex[2] -= region.z();
		return vertex;
	}
	
	private static Stream<int[]> getVertices(BlockPos pos,
		HashSet<BlockPos> matchingBlocks)
	{
		Builder<int[]> builder = Stream.<int[]> builder();
		
		if(!matchingBlocks.contains(pos.down()))
		{
			builder.accept(getVertex(pos, 0, 0, 0));
			builder.accept(getVertex(pos, 1, 0, 0));
			builder.accept(getVertex(pos, 1, 0, 1));
			builder.accept(getVertex(pos, 0, 0, 1));
		}
		
		if(!matchingBlocks.contains(pos.up()))
		{
			builder.accept(getVertex(pos, 0, 1, 0));
			builder.accept(getVertex(pos, 0, 1, 1));
			builder.accept(getVertex(pos, 1, 1, 1));
			builder.accept(getVertex(pos, 1, 1, 0));
		}
		
		if(!matchingBlocks.contains(pos.north()))
		{
			builder.accept(getVertex(pos, 0, 0, 0));
			builder.accept(getVertex(pos, 0, 1, 0));
			builder.accept(getVertex(pos, 1, 1, 0));
			builder.accept(getVertex(pos, 1, 0, 0));
		}
		
		if(!matchingBlocks.contains(pos.east()))
		{
			builder.accept(getVertex(pos, 1, 0, 0));
			builder.accept(getVertex(pos, 1, 1, 0));
			builder.accept(getVertex(pos, 1, 1, 1));
			builder.accept(getVertex(pos, 1, 0, 1));
		}
		
		if(!matchingBlocks.contains(pos.south()))
		{
			builder.accept(getVertex(pos, 0, 0, 1));
			builder.accept(getVertex(pos, 1, 0, 1));
			builder.accept(getVertex(pos, 1, 1, 1));
			builder.accept(getVertex(pos, 0, 1, 1));
		}
		
		if(!matchingBlocks.contains(pos.west()))
		{
			builder.accept(getVertex(pos, 0, 0, 0));
			builder.accept(getVertex(pos, 0, 0, 1));
			builder.accept(getVertex(pos, 0, 1, 1));
			builder.accept(getVertex(pos, 0, 1, 0));
		}
		
		return builder.build();
	}
	
	private static int[] getVertex(BlockPos pos, int x, int y, int z)
	{
		return new int[]{pos.getX() + x, pos.getY() + y, pos.getZ() + z};
	}
}
