/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.util.RenderUtils;

public final class NewChunksOutlineRenderer implements NewChunksChunkRenderer
{
	@Override
	public BufferBuilder buildBuffer(Set<ChunkPos> chunks, int drawDistance)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION);
		renderChunks(new ArrayList<>(chunks), drawDistance, bufferBuilder);
		bufferBuilder.end();
		
		return bufferBuilder;
	}
	
	private void renderChunks(List<ChunkPos> chunks, int drawDistance,
		BufferBuilder bufferBuilder)
	{
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		ChunkPos camChunkPos = new ChunkPos(camPos);
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		for(ChunkPos chunkPos : chunks)
		{
			if(chunkPos.method_24022(camChunkPos) > drawDistance)
				continue;
			
			BlockPos blockPos =
				chunkPos.getStartPos().add(-regionX, 0, -regionZ);
			float x1 = blockPos.getX() + 0.5F;
			float x2 = x1 + 15;
			float z1 = blockPos.getZ() + 0.5F;
			float z2 = z1 + 15;
			
			bufferBuilder.vertex(x1, 0, z1).next();
			bufferBuilder.vertex(x2, 0, z1).next();
			
			bufferBuilder.vertex(x2, 0, z1).next();
			bufferBuilder.vertex(x2, 0, z2).next();
			
			bufferBuilder.vertex(x2, 0, z2).next();
			bufferBuilder.vertex(x1, 0, z2).next();
			
			bufferBuilder.vertex(x1, 0, z2).next();
			bufferBuilder.vertex(x1, 0, z1).next();
		}
	}
}
