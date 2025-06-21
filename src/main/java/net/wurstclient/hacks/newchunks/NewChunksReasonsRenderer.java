/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import java.util.List;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.WurstRenderLayers;
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
	
	public void buildBuffer(VertexConsumer buffer, List<BlockPos> reasons)
	{
		ChunkPos camChunkPos = new ChunkPos(RenderUtils.getCameraBlockPos());
		RegionPos region = RegionPos.of(camChunkPos);
		int drawDistance = this.drawDistance.getValueI();
		
		for(BlockPos pos : reasons)
		{
			ChunkPos chunkPos = new ChunkPos(pos);
			if(chunkPos.getChebyshevDistance(camChunkPos) > drawDistance)
				continue;
			
			Box box = new Box(pos).offset(-region.x(), 0, -region.z());
			RenderUtils.drawSolidBox(buffer, box, 0xFFFFFFFF);
		}
	}
	
	public RenderLayer.MultiPhase getLayer()
	{
		return WurstRenderLayers.ESP_QUADS;
	}
}
