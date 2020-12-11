/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Searches a {@link Chunk} for a particular type of {@link Block}.
 */
public final class ChunkSearcher
{
	private final Chunk chunk;
	private final Block block;
	private final int dimensionId;
	private final ArrayList<BlockPos> matchingBlocks = new ArrayList<>();
	private ChunkSearcher.Status status = Status.IDLE;
	private Future<?> future;
	
	public ChunkSearcher(Chunk chunk, Block block, int dimensionId)
	{
		this.chunk = chunk;
		this.block = block;
		this.dimensionId = dimensionId;
	}
	
	public void startSearching(ExecutorService pool)
	{
		if(status != Status.IDLE)
			throw new IllegalStateException();
		
		status = Status.SEARCHING;
		future = pool.submit(() -> searchNow());
	}
	
	private void searchNow()
	{
		if(status == Status.IDLE || status == Status.DONE
			|| !matchingBlocks.isEmpty())
			throw new IllegalStateException();
		
		ChunkPos chunkPos = chunk.getPos();
		int minX = chunkPos.getStartX();
		int minY = 0;
		int minZ = chunkPos.getStartZ();
		int maxX = chunkPos.getEndX();
		int maxY = 255;
		int maxZ = chunkPos.getEndZ();
		
		for(int x = minX; x <= maxX; x++)
			for(int y = minY; y <= maxY; y++)
				for(int z = minZ; z <= maxZ; z++)
				{
					if(status == Status.INTERRUPTED || Thread.interrupted())
						return;
					
					BlockPos pos = new BlockPos(x, y, z);
					Block block = BlockUtils.getBlock(pos);
					if(!this.block.equals(block))
						continue;
					
					matchingBlocks.add(pos);
				}
			
		status = Status.DONE;
	}
	
	public void cancelSearching()
	{
		new Thread(() -> cancelNow(), "ChunkSearcher-canceller").start();
	}
	
	private void cancelNow()
	{
		if(future != null)
			try
			{
				status = Status.INTERRUPTED;
				future.get();
				
			}catch(InterruptedException | ExecutionException e)
			{
				e.printStackTrace();
			}
		
		matchingBlocks.clear();
		status = Status.IDLE;
	}
	
	public Chunk getChunk()
	{
		return chunk;
	}
	
	public Block getBlock()
	{
		return block;
	}
	
	public int getDimensionId()
	{
		return dimensionId;
	}
	
	public ArrayList<BlockPos> getMatchingBlocks()
	{
		return matchingBlocks;
	}
	
	public ChunkSearcher.Status getStatus()
	{
		return status;
	}
	
	public static enum Status
	{
		IDLE,
		SEARCHING,
		INTERRUPTED,
		DONE;
	}
}
