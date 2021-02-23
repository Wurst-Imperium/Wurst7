/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
	
	public static Callable<ArrayList<int[]>> createTask(
		HashSet<BlockPos> blocks)
	{
		return () -> blocks.parallelStream()
			.flatMap(pos -> getVertices(pos, blocks).stream())
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	private static ArrayList<int[]> getVertices(BlockPos pos,
		HashSet<BlockPos> matchingBlocks)
	{
		ArrayList<int[]> vertices = new ArrayList<>();
		
		if(!matchingBlocks.contains(pos.down()))
		{
			vertices.add(getVertex(pos, 0, 0, 0));
			vertices.add(getVertex(pos, 1, 0, 0));
			vertices.add(getVertex(pos, 1, 0, 1));
			vertices.add(getVertex(pos, 0, 0, 1));
		}
		
		if(!matchingBlocks.contains(pos.up()))
		{
			vertices.add(getVertex(pos, 0, 1, 0));
			vertices.add(getVertex(pos, 0, 1, 1));
			vertices.add(getVertex(pos, 1, 1, 1));
			vertices.add(getVertex(pos, 1, 1, 0));
		}
		
		if(!matchingBlocks.contains(pos.north()))
		{
			vertices.add(getVertex(pos, 0, 0, 0));
			vertices.add(getVertex(pos, 0, 1, 0));
			vertices.add(getVertex(pos, 1, 1, 0));
			vertices.add(getVertex(pos, 1, 0, 0));
		}
		
		if(!matchingBlocks.contains(pos.east()))
		{
			vertices.add(getVertex(pos, 1, 0, 0));
			vertices.add(getVertex(pos, 1, 1, 0));
			vertices.add(getVertex(pos, 1, 1, 1));
			vertices.add(getVertex(pos, 1, 0, 1));
		}
		
		if(!matchingBlocks.contains(pos.south()))
		{
			vertices.add(getVertex(pos, 0, 0, 1));
			vertices.add(getVertex(pos, 1, 0, 1));
			vertices.add(getVertex(pos, 1, 1, 1));
			vertices.add(getVertex(pos, 0, 1, 1));
		}
		
		if(!matchingBlocks.contains(pos.west()))
		{
			vertices.add(getVertex(pos, 0, 0, 0));
			vertices.add(getVertex(pos, 0, 0, 1));
			vertices.add(getVertex(pos, 0, 1, 1));
			vertices.add(getVertex(pos, 0, 1, 0));
		}
		
		return vertices;
	}
	
	private static int[] getVertex(BlockPos pos, int x, int y, int z)
	{
		return new int[]{pos.getX() + x, pos.getY() + y, pos.getZ() + z};
	}
}
