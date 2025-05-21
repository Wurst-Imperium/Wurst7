/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import java.util.Set;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

public final class NewChunksOutlineRenderer implements NewChunksChunkRenderer
{
	@Override
	public void buildBuffer(VertexConsumer buffer, Set<ChunkPos> chunks,
		int drawDistance)
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
			int color = 0xFFFFFFFF;
			
			RenderUtils.drawLine(buffer, x1, 0, z1, x2, 0, z1, color);
			RenderUtils.drawLine(buffer, x2, 0, z1, x2, 0, z2, color);
			RenderUtils.drawLine(buffer, x2, 0, z2, x1, 0, z2, color);
			RenderUtils.drawLine(buffer, x1, 0, z2, x1, 0, z1, color);
		}
	}
	
	@Override
	public RenderLayer.MultiPhase getLayer()
	{
		return WurstRenderLayers.ESP_LINES;
	}
}
