/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.treebot;

import java.util.ArrayList;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
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
		
		vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
		
		double boxMin = 1 / 16.0;
		double boxMax = 15 / 16.0;
		Vec3d regionOffset = RegionPos.of(stump).negate().toVec3d();
		Box box = new Box(boxMin, boxMin, boxMin, boxMax, boxMax, boxMax)
			.offset(regionOffset);
		
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
		RenderUtils.drawCrossBox(box.offset(stump), bufferBuilder);
		
		for(BlockPos log : logs)
			RenderUtils.drawOutlinedBox(box.offset(log), bufferBuilder);
		
		BuiltBuffer buffer = bufferBuilder.end();
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	public void draw(MatrixStack matrixStack)
	{
		if(vertexBuffer == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		RenderSystem.setShaderColor(0, 1, 0, 0.5F);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack, RegionPos.of(stump));
		
		Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
		Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
		ShaderProgram shader = RenderSystem.getShader();
		
		vertexBuffer.bind();
		vertexBuffer.draw(viewMatrix, projMatrix, shader);
		VertexBuffer.unbind();
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
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
