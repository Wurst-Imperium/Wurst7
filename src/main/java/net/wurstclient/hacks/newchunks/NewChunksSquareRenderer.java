/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class NewChunksSquareRenderer implements NewChunksChunkRenderer
{
	@Override
	public BuiltBuffer buildBuffer(Set<ChunkPos> chunks, int drawDistance)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		renderChunks(new ArrayList<>(chunks), drawDistance, bufferBuilder);
		return bufferBuilder.end();
	}
	
	private void renderChunks(List<ChunkPos> chunks, int drawDistance,
		BufferBuilder bufferBuilder)
	{
		ChunkPos camChunkPos = new ChunkPos(RenderUtils.getCameraBlockPos());
		RegionPos region = RegionPos.of(camChunkPos);
		
		for(ChunkPos chunkPos : chunks)
		{
			if(chunkPos.getChebyshevDistance(camChunkPos) > drawDistance)
				continue;
			
			BlockPos blockPos =
				chunkPos.getBlockPos(-region.x(), 0, -region.z());
			float x1 = blockPos.getX() + 0.5F;
			float x2 = x1 + 15;
			float z1 = blockPos.getZ() + 0.5F;
			float z2 = z1 + 15;
			
			bufferBuilder.vertex(x1, 0, z1).next();
			bufferBuilder.vertex(x2, 0, z1).next();
			bufferBuilder.vertex(x2, 0, z2).next();
			bufferBuilder.vertex(x1, 0, z2).next();
		}
	}
}
