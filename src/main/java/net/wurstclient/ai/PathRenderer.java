/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

public final class PathRenderer
{
	public static void renderArrow(MatrixStack matrixStack, BlockPos start,
		BlockPos end)
	{
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
		int startX = start.getX();
		int startY = start.getY();
		int startZ = start.getZ();
		
		int endX = end.getX();
		int endY = end.getY();
		int endZ = end.getZ();
		
		matrixStack.push();
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		
		// main line
		bufferBuilder.vertex(matrix, startX, startY, startZ).next();
		bufferBuilder.vertex(matrix, endX, endY, endZ).next();
		
		matrixStack.translate(endX, endY, endZ);
		
		float scale = 1 / 16F;
		matrixStack.scale(scale, scale, scale);
		
		int xDiff = endX - startX;
		int yDiff = endY - startY;
		int zDiff = endZ - startZ;
		
		float xAngle = (float)(Math.atan2(yDiff, -zDiff) + Math.toRadians(90));
		matrixStack.multiply(Vec3f.POSITIVE_X.getRadialQuaternion(xAngle));
		
		double yzDiff = Math.sqrt(yDiff * yDiff + zDiff * zDiff);
		float zAngle = (float)Math.atan2(xDiff, yzDiff);
		matrixStack.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(zAngle));
		
		// arrow head
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
		
		matrixStack.pop();
		
		tessellator.draw();
	}
	
	public static void renderNode(MatrixStack matrixStack, BlockPos pos)
	{
		matrixStack.push();
		
		matrixStack.translate(pos.getX(), pos.getY(), pos.getZ());
		matrixStack.scale(0.1F, 0.1F, 0.1F);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
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
		
		tessellator.draw();
		
		matrixStack.pop();
	}
}
