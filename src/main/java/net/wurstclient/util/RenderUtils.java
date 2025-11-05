/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstRenderLayers;

public enum RenderUtils
{
	;
	
	public static void applyRegionalRenderOffset(MatrixStack matrixStack)
	{
		applyRegionalRenderOffset(matrixStack, getCameraRegion());
	}
	
	public static void applyRegionalRenderOffset(MatrixStack matrixStack,
		Chunk chunk)
	{
		applyRegionalRenderOffset(matrixStack, RegionPos.of(chunk.getPos()));
	}
	
	public static void applyRegionalRenderOffset(MatrixStack matrixStack,
		RegionPos region)
	{
		Vec3d offset = region.toVec3d().subtract(getCameraPos());
		matrixStack.translate(offset.x, offset.y, offset.z);
	}
	
	public static void applyRenderOffset(MatrixStack matrixStack)
	{
		Vec3d camPos = getCameraPos();
		matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
	}
	
	public static Vec3d getCameraPos()
	{
		Camera camera = WurstClient.MC.getBlockEntityRenderDispatcher().camera;
		if(camera == null)
			return Vec3d.ZERO;
		
		return camera.getPos();
	}
	
	public static BlockPos getCameraBlockPos()
	{
		Camera camera = WurstClient.MC.getBlockEntityRenderDispatcher().camera;
		if(camera == null)
			return BlockPos.ORIGIN;
		
		return camera.getBlockPos();
	}
	
	public static RegionPos getCameraRegion()
	{
		return RegionPos.of(getCameraBlockPos());
	}
	
	public static VertexConsumerProvider.Immediate getVCP()
	{
		return WurstClient.MC.getBufferBuilders().getEntityVertexConsumers();
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
	
	public static void setShaderColor(float[] rgb, float opacity)
	{
		RenderSystem.setShaderColor(rgb[0], rgb[1], rgb[2], opacity);
	}
	
	public static int toIntColor(float[] rgb, float opacity)
	{
		return (int)(MathHelper.clamp(opacity, 0, 1) * 255) << 24
			| (int)(MathHelper.clamp(rgb[0], 0, 1) * 255) << 16
			| (int)(MathHelper.clamp(rgb[1], 0, 1) * 255) << 8
			| (int)(MathHelper.clamp(rgb[2], 0, 1) * 255);
	}
	
	public static void drawLine(MatrixStack matrices, Vec3d start, Vec3d end,
		int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d offset = getCameraPos().negate();
		drawLine(matrices, buffer, start.add(offset), end.add(offset), color);
		
		vcp.draw(layer);
	}
	
	private static Vec3d getTracerOrigin(float partialTicks)
	{
		Vec3d start = RotationUtils.getClientLookVec(partialTicks).multiply(10);
		if(WurstClient.MC.options
			.getPerspective() == Perspective.THIRD_PERSON_FRONT)
			start = start.negate();
		
		return start;
	}
	
	public static void drawTracer(MatrixStack matrices, float partialTicks,
		Vec3d end, int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d start = getTracerOrigin(partialTicks);
		Vec3d offset = getCameraPos().negate();
		drawLine(matrices, buffer, start, end.add(offset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawTracers(MatrixStack matrices, float partialTicks,
		List<Vec3d> ends, int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d start = getTracerOrigin(partialTicks);
		Vec3d offset = getCameraPos().negate();
		for(Vec3d end : ends)
			drawLine(matrices, buffer, start, end.add(offset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawTracers(MatrixStack matrices, float partialTicks,
		List<ColoredPoint> ends, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d start = getTracerOrigin(partialTicks);
		Vec3d offset = getCameraPos().negate();
		for(ColoredPoint end : ends)
			drawLine(matrices, buffer, start, end.point().add(offset),
				end.color());
		
		vcp.draw(layer);
	}
	
	public static void drawLine(MatrixStack matrices, VertexConsumer buffer,
		Vec3d start, Vec3d end, int color)
	{
		Entry entry = matrices.peek();
		float x1 = (float)start.x;
		float y1 = (float)start.y;
		float z1 = (float)start.z;
		float x2 = (float)end.x;
		float y2 = (float)end.y;
		float z2 = (float)end.z;
		drawLine(entry, buffer, x1, y1, z1, x2, y2, z2, color);
	}
	
	public static void drawLine(MatrixStack.Entry entry, VertexConsumer buffer,
		float x1, float y1, float z1, float x2, float y2, float z2, int color)
	{
		Vector3f normal = new Vector3f(x2, y2, z2).sub(x1, y1, z1).normalize();
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, normal.x,
			normal.y, normal.z);
		
		// If the line goes through the screen, add another vertex there. This
		// works around a bug in Minecraft's line shader.
		float t = new Vector3f(x1, y1, z1).negate().dot(normal);
		float length = new Vector3f(x2, y2, z2).sub(x1, y1, z1).length();
		if(t > 0 && t < length)
		{
			Vector3f closeToCam = new Vector3f(normal).mul(t).add(x1, y1, z1);
			buffer.vertex(entry, closeToCam).color(color).normal(entry,
				normal.x, normal.y, normal.z);
			buffer.vertex(entry, closeToCam).color(color).normal(entry,
				normal.x, normal.y, normal.z);
		}
		
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, normal.x,
			normal.y, normal.z);
	}
	
	public static void drawLine(VertexConsumer buffer, float x1, float y1,
		float z1, float x2, float y2, float z2, int color)
	{
		Vector3f n = new Vector3f(x2, y2, z2).sub(x1, y1, z1).normalize();
		buffer.vertex(x1, y1, z1).color(color).normal(n.x, n.y, n.z);
		buffer.vertex(x2, y2, z2).color(color).normal(n.x, n.y, n.z);
	}
	
	public static void drawCurvedLine(MatrixStack matrices, List<Vec3d> points,
		int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLineStrip(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d offset = getCameraPos().negate();
		List<Vec3d> points2 = points.stream().map(v -> v.add(offset)).toList();
		drawCurvedLine(matrices, buffer, points2, color);
		
		vcp.draw(layer);
	}
	
	public static void drawCurvedLine(MatrixStack matrices,
		VertexConsumer buffer, List<Vec3d> points, int color)
	{
		if(points.size() < 2)
			return;
		
		MatrixStack.Entry entry = matrices.peek();
		Vector3f first = points.get(0).toVector3f();
		Vector3f second = points.get(1).toVector3f();
		Vector3f normal = new Vector3f(first).sub(second).normalize();
		buffer.vertex(entry, first).color(color).normal(entry, normal.x,
			normal.y, normal.z);
		
		for(int i = 1; i < points.size(); i++)
		{
			Vector3f prev = points.get(i - 1).toVector3f();
			Vector3f current = points.get(i).toVector3f();
			normal = new Vector3f(current).sub(prev).normalize();
			buffer.vertex(entry, current).color(color).normal(entry, normal.x,
				normal.y, normal.z);
		}
	}
	
	public static void drawSolidBox(MatrixStack matrices, Box box, int color,
		boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getQuads(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawSolidBox(matrices, buffer, box.offset(getCameraPos().negate()),
			color);
		
		vcp.draw(layer);
	}
	
	public static void drawSolidBoxes(MatrixStack matrices, List<Box> boxes,
		int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getQuads(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(Box box : boxes)
			drawSolidBox(matrices, buffer, box.offset(camOffset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawSolidBoxes(MatrixStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getQuads(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(ColoredBox box : boxes)
			drawSolidBox(matrices, buffer, box.box().offset(camOffset),
				box.color());
		
		vcp.draw(layer);
	}
	
	public static void drawSolidBox(VertexConsumer buffer, Box box, int color)
	{
		drawSolidBox(new MatrixStack(), buffer, box, color);
	}
	
	public static void drawSolidBox(MatrixStack matrices, VertexConsumer buffer,
		Box box, int color)
	{
		MatrixStack.Entry entry = matrices.peek();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		buffer.vertex(entry, x1, y1, z1).color(color);
		buffer.vertex(entry, x2, y1, z1).color(color);
		buffer.vertex(entry, x2, y1, z2).color(color);
		buffer.vertex(entry, x1, y1, z2).color(color);
		
		buffer.vertex(entry, x1, y2, z1).color(color);
		buffer.vertex(entry, x1, y2, z2).color(color);
		buffer.vertex(entry, x2, y2, z2).color(color);
		buffer.vertex(entry, x2, y2, z1).color(color);
		
		buffer.vertex(entry, x1, y1, z1).color(color);
		buffer.vertex(entry, x1, y2, z1).color(color);
		buffer.vertex(entry, x2, y2, z1).color(color);
		buffer.vertex(entry, x2, y1, z1).color(color);
		
		buffer.vertex(entry, x2, y1, z1).color(color);
		buffer.vertex(entry, x2, y2, z1).color(color);
		buffer.vertex(entry, x2, y2, z2).color(color);
		buffer.vertex(entry, x2, y1, z2).color(color);
		
		buffer.vertex(entry, x1, y1, z2).color(color);
		buffer.vertex(entry, x2, y1, z2).color(color);
		buffer.vertex(entry, x2, y2, z2).color(color);
		buffer.vertex(entry, x1, y2, z2).color(color);
		
		buffer.vertex(entry, x1, y1, z1).color(color);
		buffer.vertex(entry, x1, y1, z2).color(color);
		buffer.vertex(entry, x1, y2, z2).color(color);
		buffer.vertex(entry, x1, y2, z1).color(color);
	}
	
	public static void drawOutlinedBox(MatrixStack matrices, Box box, int color,
		boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawOutlinedBox(matrices, buffer, box.offset(getCameraPos().negate()),
			color);
		
		vcp.draw(layer);
	}
	
	public static void drawOutlinedBoxes(MatrixStack matrices, List<Box> boxes,
		int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(Box box : boxes)
			drawOutlinedBox(matrices, buffer, box.offset(camOffset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawOutlinedBoxes(MatrixStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(ColoredBox box : boxes)
			drawOutlinedBox(matrices, buffer, box.box().offset(camOffset),
				box.color());
		
		vcp.draw(layer);
	}
	
	public static void drawOutlinedBox(VertexConsumer buffer, Box box,
		int color)
	{
		drawOutlinedBox(new MatrixStack(), buffer, box, color);
	}
	
	public static void drawOutlinedBox(MatrixStack matrices,
		VertexConsumer buffer, Box box, int color)
	{
		MatrixStack.Entry entry = matrices.peek();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		// bottom lines
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, 1, 0, 0);
		buffer.vertex(entry, x2, y1, z1).color(color).normal(entry, 1, 0, 0);
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x1, y1, z2).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x2, y1, z1).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x2, y1, z2).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x1, y1, z2).color(color).normal(entry, 1, 0, 0);
		buffer.vertex(entry, x2, y1, z2).color(color).normal(entry, 1, 0, 0);
		
		// top lines
		buffer.vertex(entry, x1, y2, z1).color(color).normal(entry, 1, 0, 0);
		buffer.vertex(entry, x2, y2, z1).color(color).normal(entry, 1, 0, 0);
		buffer.vertex(entry, x1, y2, z1).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x1, y2, z2).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x2, y2, z1).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, 0, 0, 1);
		buffer.vertex(entry, x1, y2, z2).color(color).normal(entry, 1, 0, 0);
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, 1, 0, 0);
		
		// side lines
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x1, y2, z1).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x2, y1, z1).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x2, y2, z1).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x1, y1, z2).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x1, y2, z2).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x2, y1, z2).color(color).normal(entry, 0, 1, 0);
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, 0, 1, 0);
	}
	
	public static void drawCrossBox(MatrixStack matrices, Box box, int color,
		boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawCrossBox(matrices, buffer, box.offset(getCameraPos().negate()),
			color);
		
		vcp.draw(layer);
	}
	
	public static void drawCrossBoxes(MatrixStack matrices, List<Box> boxes,
		int color, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(Box box : boxes)
			drawCrossBox(matrices, buffer, box.offset(camOffset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawCrossBoxes(MatrixStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(ColoredBox box : boxes)
			drawCrossBox(matrices, buffer, box.box().offset(camOffset),
				box.color());
		
		vcp.draw(layer);
	}
	
	public static void drawCrossBox(VertexConsumer buffer, Box box, int color)
	{
		drawCrossBox(new MatrixStack(), buffer, box, color);
	}
	
	public static void drawCrossBox(MatrixStack matrices, VertexConsumer buffer,
		Box box, int color)
	{
		MatrixStack.Entry entry = matrices.peek();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		// back
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, 1, 1, 0);
		buffer.vertex(entry, x2, y2, z1).color(color).normal(entry, 1, 1, 0);
		buffer.vertex(entry, x2, y1, z1).color(color).normal(entry, -1, 1, 0);
		buffer.vertex(entry, x1, y2, z1).color(color).normal(entry, -1, 1, 0);
		
		// left
		buffer.vertex(entry, x2, y1, z1).color(color).normal(entry, 0, 1, 1);
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, 0, 1, 1);
		buffer.vertex(entry, x2, y1, z2).color(color).normal(entry, 0, 1, -1);
		buffer.vertex(entry, x2, y2, z1).color(color).normal(entry, 0, 1, -1);
		
		// front
		buffer.vertex(entry, x2, y1, z2).color(color).normal(entry, -1, 1, 0);
		buffer.vertex(entry, x1, y2, z2).color(color).normal(entry, -1, 1, 0);
		buffer.vertex(entry, x1, y1, z2).color(color).normal(entry, 1, 1, 0);
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, 1, 1, 0);
		
		// right
		buffer.vertex(entry, x1, y1, z2).color(color).normal(entry, 0, 1, -1);
		buffer.vertex(entry, x1, y2, z1).color(color).normal(entry, 0, 1, -1);
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, 0, 1, 1);
		buffer.vertex(entry, x1, y2, z2).color(color).normal(entry, 0, 1, 1);
		
		// top
		buffer.vertex(entry, x1, y2, z2).color(color).normal(entry, 1, 0, -1);
		buffer.vertex(entry, x2, y2, z1).color(color).normal(entry, 1, 0, -1);
		buffer.vertex(entry, x1, y2, z1).color(color).normal(entry, 1, 0, 1);
		buffer.vertex(entry, x2, y2, z2).color(color).normal(entry, 1, 0, 1);
		
		// bottom
		buffer.vertex(entry, x2, y1, z1).color(color).normal(entry, -1, 0, 1);
		buffer.vertex(entry, x1, y1, z2).color(color).normal(entry, -1, 0, 1);
		buffer.vertex(entry, x1, y1, z1).color(color).normal(entry, 1, 0, 1);
		buffer.vertex(entry, x2, y1, z2).color(color).normal(entry, 1, 0, 1);
	}
	
	public static void drawNode(MatrixStack matrices, Box box, int color,
		boolean depthTest)
	{
		int depthFunc = depthTest ? GlConst.GL_LEQUAL : GlConst.GL_ALWAYS;
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(depthFunc);
		
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawNode(matrices, buffer, box.offset(getCameraPos().negate()), color);
		
		vcp.draw(layer);
	}
	
	public static void drawNodes(MatrixStack matrices, List<Box> boxes,
		int color, boolean depthTest)
	{
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(Box box : boxes)
			drawNode(matrices, buffer, box.offset(camOffset), color);
		
		vcp.draw(layer);
	}
	
	public static void drawNodes(MatrixStack matrices, List<ColoredBox> boxes,
		boolean depthTest)
	{
		VertexConsumerProvider.Immediate vcp = getVCP();
		RenderLayer layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3d camOffset = getCameraPos().negate();
		for(ColoredBox box : boxes)
			drawNode(matrices, buffer, box.box().offset(camOffset),
				box.color());
		
		vcp.draw(layer);
	}
	
	public static void drawNode(VertexConsumer buffer, Box box, int color)
	{
		drawNode(new MatrixStack(), buffer, box, color);
	}
	
	public static void drawNode(MatrixStack matrices, VertexConsumer buffer,
		Box box, int color)
	{
		MatrixStack.Entry entry = matrices.peek();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		float x3 = (x1 + x2) / 2F;
		float y3 = (y1 + y2) / 2F;
		float z3 = (z1 + z2) / 2F;
		
		// middle part
		drawLine(entry, buffer, x3, y3, z2, x1, y3, z3, color);
		drawLine(entry, buffer, x1, y3, z3, x3, y3, z1, color);
		drawLine(entry, buffer, x3, y3, z1, x2, y3, z3, color);
		drawLine(entry, buffer, x2, y3, z3, x3, y3, z2, color);
		
		// top part
		drawLine(entry, buffer, x3, y2, z3, x2, y3, z3, color);
		drawLine(entry, buffer, x3, y2, z3, x1, y3, z3, color);
		drawLine(entry, buffer, x3, y2, z3, x3, y3, z1, color);
		drawLine(entry, buffer, x3, y2, z3, x3, y3, z2, color);
		
		// bottom part
		drawLine(entry, buffer, x3, y1, z3, x2, y3, z3, color);
		drawLine(entry, buffer, x3, y1, z3, x1, y3, z3, color);
		drawLine(entry, buffer, x3, y1, z3, x3, y3, z1, color);
		drawLine(entry, buffer, x3, y1, z3, x3, y3, z2, color);
	}
	
	public static void drawArrow(MatrixStack matrices, VertexConsumer buffer,
		BlockPos from, BlockPos to, RegionPos region, int color)
	{
		Vec3d fromVec = from.toCenterPos().subtract(region.x(), 0, region.z());
		Vec3d toVec = to.toCenterPos().subtract(region.x(), 0, region.z());
		drawArrow(matrices, buffer, fromVec, toVec, color, 1 / 16F);
	}
	
	public static void drawArrow(VertexConsumer buffer, Vec3d from, Vec3d to,
		int color, float headSize)
	{
		drawArrow(new MatrixStack(), buffer, from, to, color, headSize);
	}
	
	public static void drawArrow(MatrixStack matrices, VertexConsumer buffer,
		Vec3d from, Vec3d to, int color, float headSize)
	{
		matrices.push();
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix = entry.getPositionMatrix();
		
		// main line
		drawLine(matrices, buffer, from, to, color);
		
		matrices.translate(to.x, to.y, to.z);
		matrices.scale(headSize, headSize, headSize);
		
		double xDiff = to.x - from.x;
		double yDiff = to.y - from.y;
		double zDiff = to.z - from.z;
		
		float xAngle = (float)(Math.atan2(yDiff, -zDiff) + Math.toRadians(90));
		matrix.rotate(xAngle, new Vector3f(1, 0, 0));
		
		double yzDiff = Math.sqrt(yDiff * yDiff + zDiff * zDiff);
		float zAngle = (float)Math.atan2(xDiff, yzDiff);
		matrix.rotate(zAngle, new Vector3f(0, 0, 1));
		
		// arrow head
		drawLine(entry, buffer, 0, 2, 1, -1, 2, 0, color);
		drawLine(entry, buffer, -1, 2, 0, 0, 2, -1, color);
		drawLine(entry, buffer, 0, 2, -1, 1, 2, 0, color);
		drawLine(entry, buffer, 1, 2, 0, 0, 2, 1, color);
		drawLine(entry, buffer, 1, 2, 0, -1, 2, 0, color);
		drawLine(entry, buffer, 0, 2, 1, 0, 2, -1, color);
		drawLine(entry, buffer, 0, 0, 0, 1, 2, 0, color);
		drawLine(entry, buffer, 0, 0, 0, -1, 2, 0, color);
		drawLine(entry, buffer, 0, 0, 0, 0, 2, -1, color);
		drawLine(entry, buffer, 0, 0, 0, 0, 2, 1, color);
		
		matrices.pop();
	}
	
	public static void drawItem(DrawContext context, ItemStack stack, int x,
		int y, boolean large)
	{
		MatrixStack matrixStack = context.getMatrices();
		
		matrixStack.push();
		matrixStack.translate(x, y, 0);
		if(large)
			matrixStack.scale(1.5F, 1.5F, 1.5F);
		else
			matrixStack.scale(0.75F, 0.75F, 0.75F);
		
		ItemStack renderStack = stack.isEmpty() || stack.getItem() == null
			? new ItemStack(Blocks.GRASS_BLOCK) : stack;
		
		DiffuseLighting.enableGuiDepthLighting();
		context.drawItem(renderStack, 0, 0);
		DiffuseLighting.disableGuiDepthLighting();
		
		matrixStack.pop();
		
		if(stack.isEmpty())
		{
			matrixStack.push();
			matrixStack.translate(x, y, 250);
			if(large)
				matrixStack.scale(2, 2, 2);
			
			TextRenderer tr = WurstClient.MC.textRenderer;
			context.drawText(tr, "?", 3, 2, 0xf0f0f0, true);
			
			matrixStack.pop();
		}
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
	
	/**
	 * Similar to {@link DrawContext#fill(int, int, int, int, int)}, but uses
	 * floating-point coordinates instead of integers.
	 */
	public static void fill2D(DrawContext context, float x1, float y1, float x2,
		float y2, int color)
	{
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer = getVCP().getBuffer(RenderLayer.getGui());
		buffer.vertex(matrix, x1, y1, 0).color(color);
		buffer.vertex(matrix, x1, y2, 0).color(color);
		buffer.vertex(matrix, x2, y2, 0).color(color);
		buffer.vertex(matrix, x2, y1, 0).color(color);
	}
	
	/**
	 * Renders the given vertices in QUADS draw mode.
	 *
	 * @apiNote Due to back-face culling, quads will be invisible if their
	 *          vertices are not supplied in counter-clockwise order.
	 */
	public static void fillQuads2D(DrawContext context, float[][] vertices,
		int color)
	{
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer = getVCP().getBuffer(RenderLayer.getGui());
		for(float[] vertex : vertices)
			buffer.vertex(matrix, vertex[0], vertex[1], 0).color(color);
	}
	
	/**
	 * Renders the given vertices in TRIANGLE_STRIP draw mode.
	 *
	 * @apiNote Due to back-face culling, triangles will be invisible if their
	 *          vertices are not supplied in counter-clockwise order.
	 */
	public static void fillTriangle2D(DrawContext context, float[][] vertices,
		int color)
	{
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer =
			getVCP().getBuffer(RenderLayer.getDebugFilledBox());
		for(float[] vertex : vertices)
			buffer.vertex(matrix, vertex[0], vertex[1], 1).color(color);
	}
	
	/**
	 * Similar to {@link DrawContext#drawHorizontalLine(int, int, int, int)} and
	 * {@link DrawContext#drawVerticalLine(int, int, int, int)}, but supports
	 * diagonal lines, uses floating-point coordinates instead of integers, is
	 * one actual pixel wide instead of one scaled pixel, uses fewer draw calls
	 * than the vanilla method, and uses a z value of 1 to ensure that lines
	 * show up above fills.
	 */
	public static void drawLine2D(DrawContext context, float x1, float y1,
		float x2, float y2, int color)
	{
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer =
			getVCP().getBuffer(WurstRenderLayers.ONE_PIXEL_LINES);
		buffer.vertex(matrix, x1, y1, 1).color(color);
		buffer.vertex(matrix, x2, y2, 1).color(color);
	}
	
	/**
	 * Similar to {@link DrawContext#drawBorder(int, int, int, int, int)}, but
	 * uses floating-point coordinates instead of integers, is one actual pixel
	 * wide instead of one scaled pixel, uses fewer draw calls than the vanilla
	 * method, and uses a z value of 1 to ensure that lines show up above fills.
	 */
	public static void drawBorder2D(DrawContext context, float x1, float y1,
		float x2, float y2, int color)
	{
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer =
			getVCP().getBuffer(WurstRenderLayers.ONE_PIXEL_LINE_STRIP);
		buffer.vertex(matrix, x1, y1, 1).color(color);
		buffer.vertex(matrix, x2, y1, 1).color(color);
		buffer.vertex(matrix, x2, y2, 1).color(color);
		buffer.vertex(matrix, x1, y2, 1).color(color);
		buffer.vertex(matrix, x1, y1, 1).color(color);
	}
	
	/**
	 * Draws a 1px border around the given polygon.
	 */
	public static void drawLineStrip2D(DrawContext context, float[][] vertices,
		int color)
	{
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer =
			getVCP().getBuffer(WurstRenderLayers.ONE_PIXEL_LINE_STRIP);
		for(float[] vertex : vertices)
			buffer.vertex(matrix, vertex[0], vertex[1], 1).color(color);
		buffer.vertex(matrix, vertices[0][0], vertices[0][1], 1).color(color);
	}
	
	/**
	 * Draws a box shadow around the given rectangle.
	 */
	public static void drawBoxShadow2D(DrawContext context, int x1, int y1,
		int x2, int y2)
	{
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// outline
		float xo1 = x1 - 0.1F;
		float xo2 = x2 + 0.1F;
		float yo1 = y1 - 0.1F;
		float yo2 = y2 + 0.1F;
		
		int outlineColor = toIntColor(acColor, 0.5F);
		drawBorder2D(context, xo1, yo1, xo2, yo2, outlineColor);
		
		// shadow
		float xs1 = x1 - 1;
		float xs2 = x2 + 1;
		float ys1 = y1 - 1;
		float ys2 = y2 + 1;
		
		int shadowColor1 = toIntColor(acColor, 0.75F);
		int shadowColor2 = 0x00000000;
		
		MatrixStack matrixStack = context.getMatrices();
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		
		VertexConsumer buffer = getVCP().getBuffer(RenderLayer.getGui());
		
		// top
		buffer.vertex(matrix, x1, y1, 0).color(shadowColor1);
		buffer.vertex(matrix, x2, y1, 0).color(shadowColor1);
		buffer.vertex(matrix, xs2, ys1, 0).color(shadowColor2);
		buffer.vertex(matrix, xs1, ys1, 0).color(shadowColor2);
		
		// left
		buffer.vertex(matrix, xs1, ys1, 0).color(shadowColor2);
		buffer.vertex(matrix, xs1, ys2, 0).color(shadowColor2);
		buffer.vertex(matrix, x1, y2, 0).color(shadowColor1);
		buffer.vertex(matrix, x1, y1, 0).color(shadowColor1);
		
		// right
		buffer.vertex(matrix, x2, y1, 0).color(shadowColor1);
		buffer.vertex(matrix, x2, y2, 0).color(shadowColor1);
		buffer.vertex(matrix, xs2, ys2, 0).color(shadowColor2);
		buffer.vertex(matrix, xs2, ys1, 0).color(shadowColor2);
		
		// bottom
		buffer.vertex(matrix, x2, y2, 0).color(shadowColor1);
		buffer.vertex(matrix, x1, y2, 0).color(shadowColor1);
		buffer.vertex(matrix, xs1, ys2, 0).color(shadowColor2);
		buffer.vertex(matrix, xs2, ys2, 0).color(shadowColor2);
	}
	
	public record ColoredPoint(Vec3d point, int color)
	{}
	
	public record ColoredBox(Box box, int color)
	{}
}
