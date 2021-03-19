/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;

public final class PathRenderer
{
	public static void renderArrow(MatrixStack matrixStack, BlockPos start,
		BlockPos end)
	{
		int startX = start.getX();
		int startY = start.getY();
		int startZ = start.getZ();
		
		int endX = end.getX();
		int endY = end.getY();
		int endZ = end.getZ();
		
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		
		matrixStack.push();
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		{
			bufferBuilder.vertex(matrix, startX, startY, startZ).next();
			bufferBuilder.vertex(matrix, endX, endY, endZ).next();
		}
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		matrixStack.translate(endX, endY, endZ);
		float scale = 1 / 16F;
		matrixStack.scale(scale, scale, scale);
		
		// GL11.glRotated(Math.toDegrees(Math.atan2(endY - startY, startZ -
		// endZ)) + 90, 1, 0, 0);
		matrixStack.multiply(new Quaternion(1, 0, 0,
			(float)(Math.toDegrees(Math.atan2(endY - startY, startZ - endZ))
				+ 90)));
		// GL11.glRotated(Math.toDegrees(Math.atan2(endX - startX,
		// Math.sqrt(Math.pow(endY - startY, 2) + Math.pow(endZ - startZ, 2)))),
		// 0, 0, 1);
		matrixStack.multiply(new Quaternion(0, 0, 1,
			(float)Math.toDegrees(Math.atan2(endX - startX, Math.sqrt(
				Math.pow(endY - startY, 2) + Math.pow(endZ - startZ, 2))))));
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		{
			bufferBuilder.vertex(matrix, 0, 2, 1).next();
			bufferBuilder.vertex(matrix, -1, 2, 0).next();
			
			bufferBuilder.vertex(matrix, -1, 2, 0).next();
			bufferBuilder.vertex(matrix, 0, 2, -1).next();
			
			bufferBuilder.vertex(matrix, 0, 2, -1).next();
			bufferBuilder.vertex(matrix, 1, 2, 0).next();
			
			bufferBuilder.vertex(matrix, 1, 2, 0).next();
			bufferBuilder.vertex(matrix, 0, 2, 1).next();
			
			bufferBuilder.vertex(matrix, 1, 2, 0).next();
			bufferBuilder.vertex(matrix, -1, 2, 0).next();
			
			bufferBuilder.vertex(matrix, 0, 2, 1).next();
			bufferBuilder.vertex(matrix, 0, 2, -1).next();
			
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, 1, 2, 0).next();
			
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, -1, 2, 0).next();
			
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, 0, 2, -1).next();
			
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, 0, 2, 1).next();
		}
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		matrixStack.pop();
	}
	
	public static void renderNode(MatrixStack matrixStack, BlockPos pos)
	{
		matrixStack.push();
		
		matrixStack.translate(pos.getX(), pos.getY(), pos.getZ());
		matrixStack.scale(0.1F, 0.1F, 0.1F);
		
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		{
			// middle part
			bufferBuilder.vertex(matrix, 0, 0, 1).next();
			bufferBuilder.vertex(matrix, -1, 0, 0).next();
			
			bufferBuilder.vertex(matrix, -1, 0, 0).next();
			bufferBuilder.vertex(matrix, 0, 0, -1).next();
			
			bufferBuilder.vertex(matrix, 0, 0, -1).next();
			bufferBuilder.vertex(matrix, 1, 0, 0).next();
			
			bufferBuilder.vertex(matrix, 1, 0, 0).next();
			bufferBuilder.vertex(matrix, 0, 0, 1).next();
			
			// top part
			bufferBuilder.vertex(matrix, 0, 1, 0).next();
			bufferBuilder.vertex(matrix, 1, 0, 0).next();
			
			bufferBuilder.vertex(matrix, 0, 1, 0).next();
			bufferBuilder.vertex(matrix, -1, 0, 0).next();
			
			bufferBuilder.vertex(matrix, 0, 1, 0).next();
			bufferBuilder.vertex(matrix, 0, 0, -1).next();
			
			bufferBuilder.vertex(matrix, 0, 1, 0).next();
			bufferBuilder.vertex(matrix, 0, 0, 1).next();
			
			// bottom part
			bufferBuilder.vertex(matrix, 0, -1, 0).next();
			bufferBuilder.vertex(matrix, 1, 0, 0).next();
			
			bufferBuilder.vertex(matrix, 0, -1, 0).next();
			bufferBuilder.vertex(matrix, -1, 0, 0).next();
			
			bufferBuilder.vertex(matrix, 0, -1, 0).next();
			bufferBuilder.vertex(matrix, 0, 0, -1).next();
			
			bufferBuilder.vertex(matrix, 0, -1, 0).next();
			bufferBuilder.vertex(matrix, 0, 0, 1).next();
		}
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		matrixStack.pop();
	}
}
