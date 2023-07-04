/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofarm;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.util.RenderUtils;

public final class AutoFarmRenderer
{
	private VertexBuffer greenBuffer;
	private VertexBuffer cyanBuffer;
	private VertexBuffer redBuffer;
	
	public void reset()
	{
		Stream.of(greenBuffer, cyanBuffer, redBuffer).filter(Objects::nonNull)
			.forEach(VertexBuffer::close);
		greenBuffer = cyanBuffer = redBuffer = null;
	}
	
	public void render()
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		RenderUtils.applyRegionalRenderOffset(regionX, regionZ);
		
		if(greenBuffer != null)
		{
			GL11.glColor4f(0, 1, 0, 0.5F);
			greenBuffer.bind();
			VertexFormats.POSITION.startDrawing(0);
			GL11.glDrawArrays(GL11.GL_LINES, 0, greenBuffer.vertexCount);
			VertexFormats.POSITION.endDrawing();
			VertexBuffer.unbind();
		}
		
		if(cyanBuffer != null)
		{
			GL11.glColor4f(0, 1, 1, 0.5F);
			cyanBuffer.bind();
			VertexFormats.POSITION.startDrawing(0);
			GL11.glDrawArrays(GL11.GL_LINES, 0, cyanBuffer.vertexCount);
			VertexFormats.POSITION.endDrawing();
			VertexBuffer.unbind();
		}
		
		if(redBuffer != null)
		{
			GL11.glColor4f(1, 0, 0, 0.5F);
			redBuffer.bind();
			VertexFormats.POSITION.startDrawing(0);
			GL11.glDrawArrays(GL11.GL_LINES, 0, redBuffer.vertexCount);
			VertexFormats.POSITION.endDrawing();
			VertexBuffer.unbind();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	public void updateVertexBuffers(List<BlockPos> blocksToHarvest,
		Set<BlockPos> plants, List<BlockPos> blocksToReplant)
	{
		BufferBuilder bufferBuilder =
			RenderSystem.renderThreadTesselator().getBuffer();
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		Vec3d regionOffset = new Vec3d(-regionX, 0, -regionZ);
		
		double boxMin = 1 / 16.0;
		double boxMax = 15 / 16.0;
		Box box = new Box(boxMin, boxMin, boxMin, boxMax, boxMax, boxMax);
		Box node = new Box(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
		
		updateGreenBuffer(blocksToHarvest, bufferBuilder, box, regionOffset);
		updateCyanBuffer(plants, bufferBuilder, node, regionOffset);
		updateRedBuffer(blocksToReplant, bufferBuilder, box, regionOffset);
	}
	
	private void updateGreenBuffer(List<BlockPos> blocksToHarvest,
		BufferBuilder bufferBuilder, Box box, Vec3d regionOffset)
	{
		if(greenBuffer != null)
			greenBuffer.close();
		
		greenBuffer = new VertexBuffer(VertexFormats.POSITION);
		bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION);
		
		for(BlockPos pos : blocksToHarvest)
		{
			Box renderBox = box.offset(pos).offset(regionOffset);
			RenderUtils.drawOutlinedBox(renderBox, bufferBuilder);
		}
		
		bufferBuilder.end();
		greenBuffer.upload(bufferBuilder);
		VertexBuffer.unbind();
	}
	
	private void updateCyanBuffer(Set<BlockPos> plants,
		BufferBuilder bufferBuilder, Box node, Vec3d regionOffset)
	{
		if(cyanBuffer != null)
			cyanBuffer.close();
		
		cyanBuffer = new VertexBuffer(VertexFormats.POSITION);
		bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION);
		
		for(BlockPos pos : plants)
		{
			Box renderNode = node.offset(pos).offset(regionOffset);
			RenderUtils.drawNode(renderNode, bufferBuilder);
		}
		
		bufferBuilder.end();
		cyanBuffer.upload(bufferBuilder);
		VertexBuffer.unbind();
	}
	
	private void updateRedBuffer(List<BlockPos> blocksToReplant,
		BufferBuilder bufferBuilder, Box box, Vec3d regionOffset)
	{
		if(redBuffer != null)
			redBuffer.close();
		
		redBuffer = new VertexBuffer(VertexFormats.POSITION);
		bufferBuilder.begin(GL11.GL_LINES, VertexFormats.POSITION);
		
		for(BlockPos pos : blocksToReplant)
		{
			Box renderBox = box.offset(pos).offset(regionOffset);
			RenderUtils.drawOutlinedBox(renderBox, bufferBuilder);
		}
		
		bufferBuilder.end();
		redBuffer.upload(bufferBuilder);
		VertexBuffer.unbind();
	}
}
