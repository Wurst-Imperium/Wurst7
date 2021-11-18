/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.WurstClient;

import java.util.function.Predicate;

/**
 * Searches a {@link Chunk} for a particular type of {@link Block}.
 */
public final class ChunkSearcher
{
	public interface MatchContainer {
		void set(int x, int y, int z);
		void clear();
	}

	private final Chunk chunk;
	private final Predicate<Block> matcher;
	private final MatchContainer matchingBlocks;
	private ChunkSearcher.Status status = Status.IDLE;

	public ChunkSearcher(Chunk chunk, Predicate<Block> blockMatcher, MatchContainer matchingBlocks)
	{
		this.chunk = chunk;
		this.matcher = blockMatcher;
		this.matchingBlocks = matchingBlocks;
	}
	
	public void search()
	{
		if(status != Status.IDLE)
			throw new IllegalStateException("Can't run search when in status " + status);
		status = Status.SEARCHING;
		matchingBlocks.clear();

		ChunkPos chunkPos = chunk.getPos();
		ClientWorld world = WurstClient.MC.world;
		
		int minX = chunkPos.getStartX();
		int minY = world.getBottomY();
		int minZ = chunkPos.getStartZ();
		int maxX = chunkPos.getEndX();
		int maxY = chunk.getHighestNonEmptySectionYOffset() + 15;
		int maxZ = chunkPos.getEndZ();

		BlockPos.Mutable pos = new BlockPos.Mutable();
		for(int x = minX; x <= maxX; x++)
		{
			pos.setX(x);
			for (int y = minY; y <= maxY; y++)
			{
				if (status == Status.INTERRUPTED || Thread.interrupted())
					return;
				pos.setY(y);
				for (int z = minZ; z <= maxZ; z++)
				{
					pos.setZ(z);
					Block block = chunk.getBlockState(pos).getBlock();
					if (this.matcher.test(block))
						matchingBlocks.set(x, y, z);
				}
			}
		}
			
		status = Status.IDLE;
	}

	public void interrupt()
	{
		status = Status.INTERRUPTED;
	}

	public enum Status
	{
		IDLE,
		SEARCHING,
		INTERRUPTED
	}
}
