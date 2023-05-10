/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import java.util.Objects;
import java.util.stream.Stream;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class ChestEspRenderer
{
	private static VertexBuffer solidBox;
	private static VertexBuffer outlinedBox;
	
	private final MatrixStack matrixStack;
	private final int regionX;
	private final int regionZ;
	private final Vec3d start;
	
	public ChestEspRenderer(MatrixStack matrixStack)
	{
		this.matrixStack = matrixStack;
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		regionX = (camPos.getX() >> 9) * 512;
		regionZ = (camPos.getZ() >> 9) * 512;
		
		start = RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos())
			.subtract(regionX, 0, regionZ);
	}
	
	public void renderBoxes(ChestEspGroup group)
	{
		float[] colorF = group.getColorF();
		
		for(Box box : group.getBoxes())
		{
			matrixStack.push();
			
			matrixStack.translate(box.minX - regionX, box.minY,
				box.minZ - regionZ);
			
			matrixStack.scale((float)(box.maxX - box.minX),
				(float)(box.maxY - box.minY), (float)(box.maxZ - box.minZ));
			
			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
			ShaderProgram shader = RenderSystem.getShader();
			
			RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.25F);
			solidBox.bind();
			solidBox.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
			
			RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
			outlinedBox.bind();
			outlinedBox.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
			
			matrixStack.pop();
		}
	}
	
	public void renderLines(ChestEspGroup group)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		float[] colorF = group.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.5F);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
		for(Box box : group.getBoxes())
		{
			Vec3d end = box.getCenter().subtract(regionX, 0, regionZ);
			
			bufferBuilder
				.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
				.next();
			
			bufferBuilder
				.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
				.next();
		}
		
		tessellator.draw();
	}
	
	public static void prepareBuffers()
	{
		closeBuffers();
		solidBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
		outlinedBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
		
		Box box = new Box(BlockPos.ORIGIN);
		RenderUtils.drawSolidBox(box, solidBox);
		RenderUtils.drawOutlinedBox(box, outlinedBox);
	}
	
	public static void closeBuffers()
	{
		Stream.of(solidBox, outlinedBox).filter(Objects::nonNull)
			.forEach(VertexBuffer::close);
	}
}
