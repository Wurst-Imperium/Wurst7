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
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class NewChunksReasonsRenderer
{
	private final SliderSetting drawDistance;
	
	public NewChunksReasonsRenderer(SliderSetting drawDistance)
	{
		this.drawDistance = drawDistance;
	}
	
	public BuiltBuffer buildBuffer(Set<BlockPos> reasons)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator
			.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		renderBlocks(new ArrayList<>(reasons), bufferBuilder);
		return bufferBuilder.end();
	}
	
	private void renderBlocks(List<BlockPos> blocks,
		BufferBuilder bufferBuilder)
	{
		ChunkPos camChunkPos = new ChunkPos(RenderUtils.getCameraBlockPos());
		RegionPos region = RegionPos.of(camChunkPos);
		int drawDistance = this.drawDistance.getValueI();
		
		for(BlockPos pos : blocks)
		{
			ChunkPos chunkPos = new ChunkPos(pos);
			if(chunkPos.getChebyshevDistance(camChunkPos) > drawDistance)
				continue;
			
			Box bb = new Box(pos).offset(-region.x(), 0, -region.z());
			float minX = (float)bb.minX;
			float minY = (float)bb.minY;
			float minZ = (float)bb.minZ;
			float maxX = (float)bb.maxX;
			float maxY = (float)bb.maxY;
			float maxZ = (float)bb.maxZ;
			
			bufferBuilder.vertex(minX, minY, minZ);
			bufferBuilder.vertex(maxX, minY, minZ);
			bufferBuilder.vertex(maxX, minY, maxZ);
			bufferBuilder.vertex(minX, minY, maxZ);
			
			bufferBuilder.vertex(minX, maxY, minZ);
			bufferBuilder.vertex(minX, maxY, maxZ);
			bufferBuilder.vertex(maxX, maxY, maxZ);
			bufferBuilder.vertex(maxX, maxY, minZ);
			
			bufferBuilder.vertex(minX, minY, minZ);
			bufferBuilder.vertex(minX, maxY, minZ);
			bufferBuilder.vertex(maxX, maxY, minZ);
			bufferBuilder.vertex(maxX, minY, minZ);
			
			bufferBuilder.vertex(maxX, minY, minZ);
			bufferBuilder.vertex(maxX, maxY, minZ);
			bufferBuilder.vertex(maxX, maxY, maxZ);
			bufferBuilder.vertex(maxX, minY, maxZ);
			
			bufferBuilder.vertex(minX, minY, maxZ);
			bufferBuilder.vertex(maxX, minY, maxZ);
			bufferBuilder.vertex(maxX, maxY, maxZ);
			bufferBuilder.vertex(minX, maxY, maxZ);
			
			bufferBuilder.vertex(minX, minY, minZ);
			bufferBuilder.vertex(minX, minY, maxZ);
			bufferBuilder.vertex(minX, maxY, maxZ);
			bufferBuilder.vertex(minX, maxY, minZ);
		}
	}
}
