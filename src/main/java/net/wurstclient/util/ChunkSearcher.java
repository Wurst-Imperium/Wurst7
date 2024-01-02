/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;

/**
 * Searches the given {@link Chunk} for blocks matching the given query.
 */
public final class ChunkSearcher
{
	private static final ExecutorService BACKGROUND_THREAD_POOL =
		MinPriorityThreadFactory.newFixedThreadPool();
	
	private final BiPredicate<BlockPos, BlockState> query;
	private final Chunk chunk;
	private final DimensionType dimension;
	
	private CompletableFuture<ArrayList<Result>> future;
	private boolean interrupted;
	
	public ChunkSearcher(Block block, Chunk chunk, DimensionType dimension)
	{
		this((pos, state) -> block == state.getBlock(), chunk, dimension);
	}
	
	public ChunkSearcher(BiPredicate<BlockPos, BlockState> query, Chunk chunk,
		DimensionType dimension)
	{
		this.query = query;
		this.chunk = chunk;
		this.dimension = dimension;
	}
	
	public void start()
	{
		if(future != null || interrupted)
			throw new IllegalStateException();
		
		future = CompletableFuture.supplyAsync(this::searchNow,
			BACKGROUND_THREAD_POOL);
	}
	
	private ArrayList<Result> searchNow()
	{
		ArrayList<Result> results = new ArrayList<>();
		ChunkPos chunkPos = chunk.getPos();
		
		int minX = chunkPos.getStartX();
		int minY = chunk.getBottomY();
		int minZ = chunkPos.getStartZ();
		int maxX = chunkPos.getEndX();
		int maxY = ChunkUtils.getHighestNonEmptySectionYOffset(chunk) + 16;
		int maxZ = chunkPos.getEndZ();
		
		for(int x = minX; x <= maxX; x++)
			for(int y = minY; y <= maxY; y++)
				for(int z = minZ; z <= maxZ; z++)
				{
					if(interrupted)
						return results;
					
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = chunk.getBlockState(pos);
					if(!query.test(pos, state))
						continue;
					
					results.add(new Result(pos, state));
				}
			
		return results;
	}
	
	public void cancel()
	{
		if(future == null || future.isDone())
			return;
		
		interrupted = true;
		future.cancel(false);
	}
	
	public ChunkPos getPos()
	{
		return chunk.getPos();
	}
	
	public DimensionType getDimension()
	{
		return dimension;
	}
	
	public Stream<Result> getMatches()
	{
		if(future == null || future.isCancelled())
			return Stream.empty();
		
		return future.join().stream();
	}
	
	public boolean isDone()
	{
		return future != null && future.isDone();
	}
	
	public record Result(BlockPos pos, BlockState state)
	{}
}
