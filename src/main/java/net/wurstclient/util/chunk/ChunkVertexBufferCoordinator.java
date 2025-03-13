/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.chunk;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.settings.ChunkAreaSetting;
import net.wurstclient.util.EasyVertexBuffer;
import net.wurstclient.util.chunk.ChunkSearcher.Result;

public final class ChunkVertexBufferCoordinator extends AbstractChunkCoordinator
{
	private final HashMap<ChunkPos, EasyVertexBuffer> buffers = new HashMap<>();
	private final Renderer renderer;
	private final DrawMode drawMode;
	private final VertexFormat format;
	
	public ChunkVertexBufferCoordinator(BiPredicate<BlockPos, BlockState> query,
		DrawMode drawMode, VertexFormat format, Renderer renderer,
		ChunkAreaSetting area)
	{
		super(query, area);
		this.renderer = Objects.requireNonNull(renderer);
		this.drawMode = drawMode;
		this.format = format;
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
		EasyVertexBuffer buffer = buffers.remove(searcher.getPos());
		if(buffer != null)
			buffer.close();
	}
	
	@Override
	public void reset()
	{
		super.reset();
		buffers.values().forEach(EasyVertexBuffer::close);
		buffers.clear();
	}
	
	public Set<Entry<ChunkPos, EasyVertexBuffer>> getBuffers()
	{
		for(ChunkSearcher searcher : searchers.values())
			buildBuffer(searcher);
		
		return Collections.unmodifiableSet(buffers.entrySet());
	}
	
	private void buildBuffer(ChunkSearcher searcher)
	{
		if(buffers.containsKey(searcher.getPos()))
			return;
		
		EasyVertexBuffer vertexBuffer = EasyVertexBuffer
			.createAndUpload(drawMode, format, buffer -> renderer
				.buildBuffer(buffer, searcher, searcher.getMatchesList()));
		
		buffers.put(searcher.getPos(), vertexBuffer);
	}
	
	public static interface Renderer
	{
		public void buildBuffer(VertexConsumer buffer, ChunkSearcher searcher,
			List<Result> results);
	}
}
