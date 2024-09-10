/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.chunk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import net.minecraft.block.BlockState;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.util.chunk.ChunkSearcher.Result;

public final class ChunkVertexBufferCoordinator extends AbstractChunkCoordinator
{
	private final HashMap<ChunkPos, VertexBuffer> buffers = new HashMap<>();
	private final BiFunction<ChunkSearcher, Iterable<Result>, BuiltBuffer> renderer;
	
	public ChunkVertexBufferCoordinator(BiPredicate<BlockPos, BlockState> query,
		BiFunction<ChunkSearcher, Iterable<Result>, BuiltBuffer> renderer,
		ChunkAreaSetting area)
	{
		super(query, area);
		this.renderer = Objects.requireNonNull(renderer);
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		ChunkPos center = ChunkUtils.getAffectedChunk(event.getPacket());
		if(center == null)
			return;
		
		for(int x = center.x - 1; x <= center.x + 1; x++)
			for(int z = center.z - 1; z <= center.z + 1; z++)
				chunksToUpdate.add(new ChunkPos(x, z));
	}
	
	@Override
	protected void onRemove(ChunkSearcher searcher)
	{
		@SuppressWarnings("resource")
		VertexBuffer buffer = buffers.remove(searcher.getPos());
		if(buffer != null)
			buffer.close();
	}
	
	@Override
	public void reset()
	{
		super.reset();
		buffers.values().forEach(VertexBuffer::close);
		buffers.clear();
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
}
