/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.treebot;

import java.util.ArrayList;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.util.RenderUtils;

public class Tree implements AutoCloseable
{
	private final BlockPos stump;
	private final ArrayList<BlockPos> logs;
	private VertexBuffer vertexBuffer;
	
	public Tree(BlockPos stump, ArrayList<BlockPos> logs)
	{
		this.stump = stump;
		this.logs = logs;
		compileBuffer();
	}
	
	public void compileBuffer()
	{
		if(vertexBuffer != null)
			vertexBuffer.close();
		
		vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		
		int regionX = (stump.getX() >> 9) * 512;
		int regionZ = (stump.getZ() >> 9) * 512;
		
		double boxMin = 1 / 16.0;
		double boxMax = 15 / 16.0;
		Box box = new Box(boxMin, boxMin, boxMin, boxMax, boxMax, boxMax);
		
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
		RenderUtils.drawCrossBox(
			box.offset(stump).offset(-regionX, 0, -regionZ), bufferBuilder);
		
		for(BlockPos log : logs)
			RenderUtils.drawOutlinedBox(
				box.offset(log).offset(-regionX, 0, -regionZ), bufferBuilder);
		
		BuiltBuffer buffer = bufferBuilder.end();
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	@Override
	public void close()
	{
		vertexBuffer.close();
	}
	
	public BlockPos getStump()
	{
		return stump;
	}
	
	public ArrayList<BlockPos> getLogs()
	{
		return logs;
	}
	
	public VertexBuffer getVertexBuffer()
	{
		return vertexBuffer;
	}
}
