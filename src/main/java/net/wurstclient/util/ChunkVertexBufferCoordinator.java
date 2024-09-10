/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.WurstClient;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.util.ChunkSearcher.Result;

public final class ChunkVertexBufferCoordinator implements PacketInputListener
{
	private final HashMap<ChunkPos, ChunkSearcher> searchers = new HashMap<>();
	private final HashMap<ChunkPos, VertexBuffer> buffers = new HashMap<>();
	private final ChunkAreaSetting area;
	private BiPredicate<BlockPos, BlockState> query;
	private final BiFunction<ChunkSearcher, Iterable<Result>, BuiltBuffer> renderer;
	
	private final Set<ChunkPos> chunksToUpdate =
		Collections.synchronizedSet(new HashSet<>());
	
	public ChunkVertexBufferCoordinator(BiPredicate<BlockPos, BlockState> query,
		BiFunction<ChunkSearcher, Iterable<Result>, BuiltBuffer> renderer,
		ChunkAreaSetting area)
	{
		this.query = Objects.requireNonNull(query);
		this.renderer = Objects.requireNonNull(renderer);
		this.area = Objects.requireNonNull(area);
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ChunkPos chunkPos = ChunkUtils.getAffectedChunk(event.getPacket());
		
		if(chunkPos != null)
			chunksToUpdate.add(chunkPos);
	}
	
	public boolean update()
	{
		DimensionType dimension = WurstClient.MC.world.getDimension();
		HashSet<ChunkPos> chunkUpdates = clearChunksToUpdate();
		boolean searchersChanged = false;
		
		// remove outdated ChunkSearchers
		for(ChunkSearcher searcher : new ArrayList<>(searchers.values()))
		{
			boolean remove = false;
			ChunkPos searcherPos = searcher.getPos();
			
			// wrong dimension
			if(dimension != searcher.getDimension())
				remove = true;
			
			// out of range
			else if(!area.isInRange(searcherPos))
				remove = true;
			
			// chunk update
			else if(chunkUpdates.contains(searcherPos))
				remove = true;
			
			if(remove)
			{
				searchers.remove(searcherPos);
				searcher.cancel();
				VertexBuffer buffer = buffers.remove(searcherPos);
				if(buffer != null)
					buffer.close();
				searchersChanged = true;
			}
		}
		
		// add new ChunkSearchers
		for(Chunk chunk : area.getChunksInRange())
		{
			ChunkPos chunkPos = chunk.getPos();
			if(searchers.containsKey(chunkPos))
				continue;
			
			ChunkSearcher searcher = new ChunkSearcher(query, chunk, dimension);
			searchers.put(chunkPos, searcher);
			searcher.start();
			searchersChanged = true;
		}
		
		return searchersChanged;
	}
	
	public void reset()
	{
		searchers.values().forEach(ChunkSearcher::cancel);
		searchers.clear();
		buffers.values().forEach(VertexBuffer::close);
		buffers.clear();
		chunksToUpdate.clear();
	}
	
	public boolean isDone()
	{
		return searchers.values().stream().allMatch(ChunkSearcher::isDone);
	}
	
	public Set<Entry<ChunkPos, VertexBuffer>> getBuffers()
	{
		for(ChunkSearcher searcher : searchers.values())
			buildBuffer(searcher);
		
		return Collections.unmodifiableSet(buffers.entrySet());
	}
	
	private void buildBuffer(ChunkSearcher searcher)
	{
		if(buffers.containsKey(searcher.getPos()))
			return;
		
		BuiltBuffer buffer =
			renderer.apply(searcher, searcher.getMatchesList());
		if(buffer == null)
			return;
		
		VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
		buffers.put(searcher.getPos(), vertexBuffer);
	}
	
	public void setTargetBlock(Block block)
	{
		setQuery((pos, state) -> block == state.getBlock());
	}
	
	public void setQuery(BiPredicate<BlockPos, BlockState> query)
	{
		this.query = Objects.requireNonNull(query);
		searchers.values().forEach(ChunkSearcher::cancel);
		searchers.clear();
	}
	
	private HashSet<ChunkPos> clearChunksToUpdate()
	{
		synchronized(chunksToUpdate)
		{
			HashSet<ChunkPos> chunks = new HashSet<>(chunksToUpdate);
			chunksToUpdate.clear();
			return chunks;
		}
	}
}
