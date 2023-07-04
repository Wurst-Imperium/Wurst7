/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.WurstClient;

public enum RenderUtils
{
	;
	
	private static final Box DEFAULT_BOX = new Box(0, 0, 0, 1, 1, 1);
	
	public static void scissorBox(int startX, int startY, int endX, int endY)
	{
		int width = endX - startX;
		int height = endY - startY;
		int bottomY = WurstClient.MC.currentScreen.height - endY;
		double factor = WurstClient.MC.getWindow().getScaleFactor();
		
		int scissorX = (int)(startX * factor);
		int scissorY = (int)(bottomY * factor);
		int scissorWidth = (int)(width * factor);
		int scissorHeight = (int)(height * factor);
		GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
	}
	
	public static void applyRenderOffset()
	{
		applyCameraRotationOnly();
		Vec3d camPos = getCameraPos();
		
		GL11.glTranslated(-camPos.x, -camPos.y, -camPos.z);
	}
	
	public static void applyRegionalRenderOffset()
	{
		applyCameraRotationOnly();
		
		Vec3d camPos = getCameraPos();
		BlockPos blockPos = getCameraBlockPos();
		
		int regionX = (blockPos.getX() >> 9) * 512;
		int regionZ = (blockPos.getZ() >> 9) * 512;
		
		GL11.glTranslated(regionX - camPos.x, -camPos.y, regionZ - camPos.z);
	}
	
	public static void applyRegionalRenderOffset(int regionX, int regionZ)
	{
		applyCameraRotationOnly();
		
		Vec3d camPos = getCameraPos();
		GL11.glTranslated(regionX - camPos.x, -camPos.y, regionZ - camPos.z);
	}
	
	public static void applyRegionalRenderOffset(Chunk chunk)
	{
		applyCameraRotationOnly();
		
		Vec3d camPos = getCameraPos();
		
		int regionX = (chunk.getPos().getStartX() >> 9) * 512;
		int regionZ = (chunk.getPos().getStartZ() >> 9) * 512;
		
		GL11.glTranslated(regionX - camPos.x, -camPos.y, regionZ - camPos.z);
	}
	
	public static void applyCameraRotationOnly()
	{
		Camera camera = BlockEntityRenderDispatcher.INSTANCE.camera;
		GL11.glRotated(MathHelper.wrapDegrees(camera.getPitch()), 1, 0, 0);
		GL11.glRotated(MathHelper.wrapDegrees(camera.getYaw() + 180.0), 0, 1,
			0);
	}
	
	public static Vec3d getCameraPos()
	{
		Camera camera = BlockEntityRenderDispatcher.INSTANCE.camera;
		if(camera == null)
			return Vec3d.ZERO;
		
		return camera.getPos();
	}
	
	public static BlockPos getCameraBlockPos()
	{
		Camera camera = BlockEntityRenderDispatcher.INSTANCE.camera;
		if(camera == null)
			return BlockPos.ORIGIN;
		
		return camera.getBlockPos();
	}
	
	public static float[] getRainbowColor()
	{
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float pi = (float)Math.PI;
		
		float[] rainbow = new float[3];
		rainbow[0] = 0.5F + 0.5F * MathHelper.sin(x * pi);
		rainbow[1] = 0.5F + 0.5F * MathHelper.sin((x + 4F / 3F) * pi);
		rainbow[2] = 0.5F + 0.5F * MathHelper.sin((x + 8F / 3F) * pi);
		return rainbow;
	}
	
	public static void drawSolidBox()
	{
		drawSolidBox(DEFAULT_BOX);
	}
	
	public static void drawSolidBox(Box bb)
	{
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		GL11.glEnd();
	}
	
	public static void drawSolidBox(Box bb, BufferBuilder bufferBuilder)
	{
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
	}
	
	public static void drawOutlinedBox()
	{
		drawOutlinedBox(DEFAULT_BOX);
	}
	
	public static void drawOutlinedBox(Box bb)
	{
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		GL11.glEnd();
	}
	
	public static void drawOutlinedBox(Box bb, BufferBuilder bufferBuilder)
	{
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
	}
	
	public static void drawCrossBox()
	{
		drawCrossBox(DEFAULT_BOX);
	}
	
	public static void drawCrossBox(Box bb)
	{
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
		GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
		
		GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
		GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
		GL11.glEnd();
	}
	
	public static void drawCrossBox(Box bb, BufferBuilder bufferBuilder)
	{
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
		bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
		
		bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
	}
	
	public static void drawNode(Box bb)
	{
		double midX = (bb.minX + bb.maxX) / 2;
		double midY = (bb.minY + bb.maxY) / 2;
		double midZ = (bb.minZ + bb.maxZ) / 2;
		
		GL11.glVertex3d(midX, midY, bb.maxZ);
		GL11.glVertex3d(bb.minX, midY, midZ);
		
		GL11.glVertex3d(bb.minX, midY, midZ);
		GL11.glVertex3d(midX, midY, bb.minZ);
		
		GL11.glVertex3d(midX, midY, bb.minZ);
		GL11.glVertex3d(bb.maxX, midY, midZ);
		
		GL11.glVertex3d(bb.maxX, midY, midZ);
		GL11.glVertex3d(midX, midY, bb.maxZ);
		
		GL11.glVertex3d(midX, bb.maxY, midZ);
		GL11.glVertex3d(bb.maxX, midY, midZ);
		
		GL11.glVertex3d(midX, bb.maxY, midZ);
		GL11.glVertex3d(bb.minX, midY, midZ);
		
		GL11.glVertex3d(midX, bb.maxY, midZ);
		GL11.glVertex3d(midX, midY, bb.minZ);
		
		GL11.glVertex3d(midX, bb.maxY, midZ);
		GL11.glVertex3d(midX, midY, bb.maxZ);
		
		GL11.glVertex3d(midX, bb.minY, midZ);
		GL11.glVertex3d(bb.maxX, midY, midZ);
		
		GL11.glVertex3d(midX, bb.minY, midZ);
		GL11.glVertex3d(bb.minX, midY, midZ);
		
		GL11.glVertex3d(midX, bb.minY, midZ);
		GL11.glVertex3d(midX, midY, bb.minZ);
		
		GL11.glVertex3d(midX, bb.minY, midZ);
		GL11.glVertex3d(midX, midY, bb.maxZ);
	}
	
	public static void drawNode(Box bb, BufferBuilder bufferBuilder)
	{
		double midX = (bb.minX + bb.maxX) / 2;
		double midY = (bb.minY + bb.maxY) / 2;
		double midZ = (bb.minZ + bb.maxZ) / 2;
		
		bufferBuilder.vertex(midX, midY, bb.maxZ).next();
		bufferBuilder.vertex(bb.minX, midY, midZ).next();
		
		bufferBuilder.vertex(bb.minX, midY, midZ).next();
		bufferBuilder.vertex(midX, midY, bb.minZ).next();
		
		bufferBuilder.vertex(midX, midY, bb.minZ).next();
		bufferBuilder.vertex(bb.maxX, midY, midZ).next();
		
		bufferBuilder.vertex(bb.maxX, midY, midZ).next();
		bufferBuilder.vertex(midX, midY, bb.maxZ).next();
		
		bufferBuilder.vertex(midX, bb.maxY, midZ).next();
		bufferBuilder.vertex(bb.maxX, midY, midZ).next();
		
		bufferBuilder.vertex(midX, bb.maxY, midZ).next();
		bufferBuilder.vertex(bb.minX, midY, midZ).next();
		
		bufferBuilder.vertex(midX, bb.maxY, midZ).next();
		bufferBuilder.vertex(midX, midY, bb.minZ).next();
		
		bufferBuilder.vertex(midX, bb.maxY, midZ).next();
		bufferBuilder.vertex(midX, midY, bb.maxZ).next();
		
		bufferBuilder.vertex(midX, bb.minY, midZ).next();
		bufferBuilder.vertex(bb.maxX, midY, midZ).next();
		
		bufferBuilder.vertex(midX, bb.minY, midZ).next();
		bufferBuilder.vertex(bb.minX, midY, midZ).next();
		
		bufferBuilder.vertex(midX, bb.minY, midZ).next();
		bufferBuilder.vertex(midX, midY, bb.minZ).next();
		
		bufferBuilder.vertex(midX, bb.minY, midZ).next();
		bufferBuilder.vertex(midX, midY, bb.maxZ).next();
	}
	
	public static void drawArrow(Vec3d from, Vec3d to)
	{
		double startX = from.x;
		double startY = from.y;
		double startZ = from.z;
		
		double endX = to.x;
		double endY = to.y;
		double endZ = to.z;
		
		GL11.glPushMatrix();
		
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(startX, startY, startZ);
		GL11.glVertex3d(endX, endY, endZ);
		GL11.glEnd();
		
		GL11.glTranslated(endX, endY, endZ);
		GL11.glScaled(0.1, 0.1, 0.1);
		
		double angleX = Math.atan2(endY - startY, startZ - endZ);
		GL11.glRotated(Math.toDegrees(angleX) + 90, 1, 0, 0);
		
		double angleZ = Math.atan2(endX - startX,
			Math.sqrt(Math.pow(endY - startY, 2) + Math.pow(endZ - startZ, 2)));
		GL11.glRotated(Math.toDegrees(angleZ), 0, 0, 1);
		
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex3d(0, 2, 1);
		GL11.glVertex3d(-1, 2, 0);
		
		GL11.glVertex3d(-1, 2, 0);
		GL11.glVertex3d(0, 2, -1);
		
		GL11.glVertex3d(0, 2, -1);
		GL11.glVertex3d(1, 2, 0);
		
		GL11.glVertex3d(1, 2, 0);
		GL11.glVertex3d(0, 2, 1);
		
		GL11.glVertex3d(1, 2, 0);
		GL11.glVertex3d(-1, 2, 0);
		
		GL11.glVertex3d(0, 2, 1);
		GL11.glVertex3d(0, 2, -1);
		
		GL11.glVertex3d(0, 0, 0);
		GL11.glVertex3d(1, 2, 0);
		
		GL11.glVertex3d(0, 0, 0);
		GL11.glVertex3d(-1, 2, 0);
		
		GL11.glVertex3d(0, 0, 0);
		GL11.glVertex3d(0, 2, -1);
		
		GL11.glVertex3d(0, 0, 0);
		GL11.glVertex3d(0, 2, 1);
		GL11.glEnd();
		
		GL11.glPopMatrix();
	}
	
	public static void drawItem(MatrixStack matrixStack, ItemStack stack, int x,
		int y, boolean large)
	{
		RenderSystem.pushMatrix();
		RenderSystem.translated(x, y, 0);
		if(large)
			RenderSystem.scaled(1.5F, 1.5F, 1.5F);
		else
			RenderSystem.scaled(0.75F, 0.75F, 0.75F);
		
		ItemStack renderStack =
			stack.isEmpty() ? new ItemStack(Blocks.GRASS_BLOCK) : stack;
		
		DiffuseLighting.enableGuiDepthLighting();
		WurstClient.MC.getItemRenderer().renderInGuiWithOverrides(renderStack,
			0, 0);
		DiffuseLighting.disableGuiDepthLighting();
		
		RenderSystem.popMatrix();
		
		if(stack.isEmpty())
		{
			RenderSystem.pushMatrix();
			RenderSystem.translated(x, y, 0);
			if(large)
				RenderSystem.scaled(2, 2, 2);
			
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			TextRenderer fr = WurstClient.MC.textRenderer;
			fr.drawWithShadow(matrixStack, "?", 3, 2, 0xf0f0f0);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			
			RenderSystem.popMatrix();
		}
	}
}
