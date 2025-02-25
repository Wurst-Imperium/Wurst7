/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.treebot;

import java.util.ArrayList;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.util.RegionPos;
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
		
		vertexBuffer = VertexBuffer.createAndUpload(DrawMode.LINES,
			VertexFormats.LINES, this::buildBuffer);
	}
	
	private void buildBuffer(VertexConsumer buffer)
	{
		int green = 0x8000FF00;
		Box box = new Box(RegionPos.of(stump).negate().toBlockPos())
			.contract(1 / 16.0);
		
		RenderUtils.drawCrossBox(buffer, box.offset(stump), green);
		
		for(BlockPos log : logs)
			RenderUtils.drawOutlinedBox(buffer, box.offset(log), green);
	}
	
	public void draw(MatrixStack matrixStack)
	{
		if(vertexBuffer == null)
			return;
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack, RegionPos.of(stump));
		
		RenderUtils.drawBuffer(matrixStack, vertexBuffer,
			WurstRenderLayers.ESP_LINES);
		
		matrixStack.pop();
	}
	
	@Override
	public void close()
	{
		vertexBuffer.close();
		vertexBuffer = null;
	}
	
	public BlockPos getStump()
	{
		return stump;
	}
	
	public ArrayList<BlockPos> getLogs()
	{
		return logs;
	}
}
