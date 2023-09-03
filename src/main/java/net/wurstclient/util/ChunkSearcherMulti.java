/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.WurstClient;

/**
 * Searches a {@link Chunk} for list of {@link Block}.
 */
public final class ChunkSearcherMulti
{
	private final Chunk chunk;
	private final ArrayList<Block> blockList;
	private final int dimensionId;
	private final ArrayList<Result> matchingBlocks = new ArrayList<>();
	private ChunkSearcherMulti.Status status = Status.IDLE;
	private Future<?> future;
	
	public ChunkSearcherMulti(Chunk chunk, ArrayList<Block> blockList,
		int dimensionId)
	{
		this.chunk = chunk;
		this.blockList = blockList;
		this.dimensionId = dimensionId;
	}
	
	public void startSearching(ExecutorService pool)
	{
		if(status != Status.IDLE)
			throw new IllegalStateException();
		
		status = Status.SEARCHING;
		future = pool.submit(this::searchNow);
	}
	
	private void searchNow()
	{
		if(status == Status.IDLE || status == Status.DONE
			|| !matchingBlocks.isEmpty())
			throw new IllegalStateException();
		
		ChunkPos chunkPos = chunk.getPos();
		ClientWorld world = WurstClient.MC.world;
		
		int minX = chunkPos.getStartX();
		int minY = world.getBottomY();
		int minZ = chunkPos.getStartZ();
		int maxX = chunkPos.getEndX();
		int maxY = world.getTopY();
		int maxZ = chunkPos.getEndZ();
		
		for(int x = minX; x <= maxX; x++)
			for(int y = minY; y <= maxY; y++)
				for(int z = minZ; z <= maxZ; z++)
				{
					if(status == Status.INTERRUPTED || Thread.interrupted())
						return;
					
					BlockPos pos = new BlockPos(x, y, z);
					Block block = BlockUtils.getBlock(pos);
					if(!blockList.contains(block))
						continue;
					
					Result posWithType = new Result(block, pos);
					matchingBlocks.add(posWithType);
				}
			
		status = Status.DONE;
	}
	
	public void cancelSearching()
	{
		new Thread(this::cancelNow, "ChunkSearcher-canceller").start();
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
	
	public ArrayList<Block> getBlockList()
	{
		return blockList;
	}
	
	public int getDimensionId()
	{
		return dimensionId;
	}
	
	public ArrayList<Result> getMatchingBlocks()
	{
		return matchingBlocks;
	}
	
	public ChunkSearcherMulti.Status getStatus()
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
	
	public static class Result
	{
		private Block block;
		private BlockPos pos;
		
		public Result(Block block, BlockPos pos)
		{
			this.block = block;
			this.pos = pos;
		}
		
		public Block getBlock()
		{
			return block;
		}
		
		public BlockPos getPos()
		{
			return pos;
		}
	}
}
