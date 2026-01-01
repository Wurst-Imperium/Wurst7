/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.List;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.WurstRenderLayers;

public enum RenderUtils
{
	;
	
	public static void applyRegionalRenderOffset(PoseStack matrixStack)
	{
		applyRegionalRenderOffset(matrixStack, getCameraRegion());
	}
	
	public static void applyRegionalRenderOffset(PoseStack matrixStack,
		ChunkAccess chunk)
	{
		applyRegionalRenderOffset(matrixStack, RegionPos.of(chunk.getPos()));
	}
	
	public static void applyRegionalRenderOffset(PoseStack matrixStack,
		RegionPos region)
	{
		Vec3 offset = region.toVec3d().subtract(getCameraPos());
		matrixStack.translate(offset.x, offset.y, offset.z);
	}
	
	public static void applyRenderOffset(PoseStack matrixStack)
	{
		Vec3 camPos = getCameraPos();
		matrixStack.translate(-camPos.x, -camPos.y, -camPos.z);
	}
	
	public static Vec3 getCameraPos()
	{
		Camera camera = WurstClient.MC.gameRenderer.getMainCamera();
		if(camera == null)
			return Vec3.ZERO;
		
		return camera.position();
	}
	
	public static BlockPos getCameraBlockPos()
	{
		Camera camera = WurstClient.MC.gameRenderer.getMainCamera();
		if(camera == null)
			return BlockPos.ZERO;
		
		return camera.blockPosition();
	}
	
	public static RegionPos getCameraRegion()
	{
		return RegionPos.of(getCameraBlockPos());
	}
	
	public static MultiBufferSource.BufferSource getVCP()
	{
		return WurstClient.MC.renderBuffers().bufferSource();
	}
	
	public static float[] getRainbowColor()
	{
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float pi = (float)Math.PI;
		
		float[] rainbow = new float[3];
		rainbow[0] = 0.5F + 0.5F * Mth.sin(x * pi);
		rainbow[1] = 0.5F + 0.5F * Mth.sin((x + 4F / 3F) * pi);
		rainbow[2] = 0.5F + 0.5F * Mth.sin((x + 8F / 3F) * pi);
		return rainbow;
	}
	
	public static int toIntColor(float[] rgb, float opacity)
	{
		return (int)(Mth.clamp(opacity, 0, 1) * 255) << 24
			| (int)(Mth.clamp(rgb[0], 0, 1) * 255) << 16
			| (int)(Mth.clamp(rgb[1], 0, 1) * 255) << 8
			| (int)(Mth.clamp(rgb[2], 0, 1) * 255);
	}
	
	public static void drawLine(PoseStack matrices, Vec3 start, Vec3 end,
		int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 offset = getCameraPos().reverse();
		drawLine(matrices, buffer, start.add(offset), end.add(offset), color);
		
		vcp.endBatch(layer);
	}
	
	private static Vec3 getTracerOrigin(float partialTicks)
	{
		Vec3 start = RotationUtils.getClientLookVec(partialTicks).scale(10);
		if(WurstClient.MC.options
			.getCameraType() == CameraType.THIRD_PERSON_FRONT)
			start = start.reverse();
		
		return start;
	}
	
	public static void drawTracer(PoseStack matrices, float partialTicks,
		Vec3 end, int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 start = getTracerOrigin(partialTicks);
		Vec3 offset = getCameraPos().reverse();
		drawLine(matrices, buffer, start, end.add(offset), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawTracers(PoseStack matrices, float partialTicks,
		List<Vec3> ends, int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 start = getTracerOrigin(partialTicks);
		Vec3 offset = getCameraPos().reverse();
		for(Vec3 end : ends)
			drawLine(matrices, buffer, start, end.add(offset), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawTracers(PoseStack matrices, float partialTicks,
		List<ColoredPoint> ends, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 start = getTracerOrigin(partialTicks);
		Vec3 offset = getCameraPos().reverse();
		for(ColoredPoint end : ends)
			drawLine(matrices, buffer, start, end.point().add(offset),
				end.color());
		
		vcp.endBatch(layer);
	}
	
	public static void drawLine(PoseStack matrices, VertexConsumer buffer,
		Vec3 start, Vec3 end, int color)
	{
		Pose entry = matrices.last();
		float x1 = (float)start.x;
		float y1 = (float)start.y;
		float z1 = (float)start.z;
		float x2 = (float)end.x;
		float y2 = (float)end.y;
		float z2 = (float)end.z;
		drawLine(entry, buffer, x1, y1, z1, x2, y2, z2, color);
	}
	
	public static void drawLine(PoseStack.Pose entry, VertexConsumer buffer,
		float x1, float y1, float z1, float x2, float y2, float z2, int color)
	{
		Vector3f normal = new Vector3f(x2, y2, z2).sub(x1, y1, z1).normalize();
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, normal).setLineWidth(2);
		
		// If the line goes through the screen, add another vertex there. This
		// works around a bug in Minecraft's line shader.
		float t = new Vector3f(x1, y1, z1).negate().dot(normal);
		float length = new Vector3f(x2, y2, z2).sub(x1, y1, z1).length();
		if(t > 0 && t < length)
		{
			Vector3f closeToCam = new Vector3f(normal).mul(t).add(x1, y1, z1);
			buffer.addVertex(entry, closeToCam).setColor(color)
				.setNormal(entry, normal).setLineWidth(2);
			buffer.addVertex(entry, closeToCam).setColor(color)
				.setNormal(entry, normal).setLineWidth(2);
		}
		
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, normal).setLineWidth(2);
	}
	
	public static void drawLine(VertexConsumer buffer, float x1, float y1,
		float z1, float x2, float y2, float z2, int color)
	{
		Vector3f n = new Vector3f(x2, y2, z2).sub(x1, y1, z1).normalize();
		buffer.addVertex(x1, y1, z1).setColor(color).setNormal(n.x, n.y, n.z)
			.setLineWidth(2);
		buffer.addVertex(x2, y2, z2).setColor(color).setNormal(n.x, n.y, n.z)
			.setLineWidth(2);
	}
	
	public static void drawCurvedLine(PoseStack matrices, List<Vec3> points,
		int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 offset = getCameraPos().reverse();
		List<Vec3> points2 = points.stream().map(v -> v.add(offset)).toList();
		drawCurvedLine(matrices, buffer, points2, color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawCurvedLine(PoseStack matrices, VertexConsumer buffer,
		List<Vec3> points, int color)
	{
		if(points.size() < 2)
			return;
		
		PoseStack.Pose entry = matrices.last();
		
		for(int i = 1; i < points.size(); i++)
		{
			Vector3f prev = points.get(i - 1).toVector3f();
			Vector3f current = points.get(i).toVector3f();
			Vector3f normal = new Vector3f(current).sub(prev).normalize();
			buffer.addVertex(entry, prev).setColor(color)
				.setNormal(entry, normal).setLineWidth(2);
			buffer.addVertex(entry, current).setColor(color)
				.setNormal(entry, normal).setLineWidth(2);
		}
	}
	
	public static void drawSolidBox(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getQuads(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawSolidBox(matrices, buffer, box.move(getCameraPos().reverse()),
			color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawSolidBoxes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getQuads(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(AABB box : boxes)
			drawSolidBox(matrices, buffer, box.move(camOffset), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawSolidBoxes(PoseStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getQuads(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(ColoredBox box : boxes)
			drawSolidBox(matrices, buffer, box.box().move(camOffset),
				box.color());
		
		vcp.endBatch(layer);
	}
	
	public static void drawSolidBox(VertexConsumer buffer, AABB box, int color)
	{
		drawSolidBox(new PoseStack(), buffer, box, color);
	}
	
	public static void drawSolidBox(PoseStack matrices, VertexConsumer buffer,
		AABB box, int color)
	{
		PoseStack.Pose entry = matrices.last();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		buffer.addVertex(entry, x1, y1, z1).setColor(color);
		buffer.addVertex(entry, x2, y1, z1).setColor(color);
		buffer.addVertex(entry, x2, y1, z2).setColor(color);
		buffer.addVertex(entry, x1, y1, z2).setColor(color);
		
		buffer.addVertex(entry, x1, y2, z1).setColor(color);
		buffer.addVertex(entry, x1, y2, z2).setColor(color);
		buffer.addVertex(entry, x2, y2, z2).setColor(color);
		buffer.addVertex(entry, x2, y2, z1).setColor(color);
		
		buffer.addVertex(entry, x1, y1, z1).setColor(color);
		buffer.addVertex(entry, x1, y2, z1).setColor(color);
		buffer.addVertex(entry, x2, y2, z1).setColor(color);
		buffer.addVertex(entry, x2, y1, z1).setColor(color);
		
		buffer.addVertex(entry, x2, y1, z1).setColor(color);
		buffer.addVertex(entry, x2, y2, z1).setColor(color);
		buffer.addVertex(entry, x2, y2, z2).setColor(color);
		buffer.addVertex(entry, x2, y1, z2).setColor(color);
		
		buffer.addVertex(entry, x1, y1, z2).setColor(color);
		buffer.addVertex(entry, x2, y1, z2).setColor(color);
		buffer.addVertex(entry, x2, y2, z2).setColor(color);
		buffer.addVertex(entry, x1, y2, z2).setColor(color);
		
		buffer.addVertex(entry, x1, y1, z1).setColor(color);
		buffer.addVertex(entry, x1, y1, z2).setColor(color);
		buffer.addVertex(entry, x1, y2, z2).setColor(color);
		buffer.addVertex(entry, x1, y2, z1).setColor(color);
	}
	
	public static void drawOutlinedBox(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawOutlinedBox(matrices, buffer, box.move(getCameraPos().reverse()),
			color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawOutlinedBoxes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(AABB box : boxes)
			drawOutlinedBox(matrices, buffer, box.move(camOffset), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawOutlinedBoxes(PoseStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(ColoredBox box : boxes)
			drawOutlinedBox(matrices, buffer, box.box().move(camOffset),
				box.color());
		
		vcp.endBatch(layer);
	}
	
	public static void drawOutlinedBox(VertexConsumer buffer, AABB box,
		int color)
	{
		drawOutlinedBox(new PoseStack(), buffer, box, color);
	}
	
	public static void drawOutlinedBox(PoseStack matrices,
		VertexConsumer buffer, AABB box, int color)
	{
		PoseStack.Pose entry = matrices.last();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		// bottom lines
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z1).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z2).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z1).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z2).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z2).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z2).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		
		// top lines
		buffer.addVertex(entry, x1, y2, z1).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z1).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z1).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z2).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z1).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, 0, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z2).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, 1, 0, 0).setLineWidth(2);
		
		// side lines
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z1).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z1).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z1).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z2).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z2).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z2).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, 0, 1, 0).setLineWidth(2);
	}
	
	public static void drawCrossBox(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawCrossBox(matrices, buffer, box.move(getCameraPos().reverse()),
			color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawCrossBoxes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(AABB box : boxes)
			drawCrossBox(matrices, buffer, box.move(camOffset), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawCrossBoxes(PoseStack matrices,
		List<ColoredBox> boxes, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(ColoredBox box : boxes)
			drawCrossBox(matrices, buffer, box.box().move(camOffset),
				box.color());
		
		vcp.endBatch(layer);
	}
	
	public static void drawCrossBox(VertexConsumer buffer, AABB box, int color)
	{
		drawCrossBox(new PoseStack(), buffer, box, color);
	}
	
	public static void drawCrossBox(PoseStack matrices, VertexConsumer buffer,
		AABB box, int color)
	{
		PoseStack.Pose entry = matrices.last();
		float x1 = (float)box.minX;
		float y1 = (float)box.minY;
		float z1 = (float)box.minZ;
		float x2 = (float)box.maxX;
		float y2 = (float)box.maxY;
		float z2 = (float)box.maxZ;
		
		// back
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, 1, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z1).setColor(color)
			.setNormal(entry, 1, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z1).setColor(color)
			.setNormal(entry, -1, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z1).setColor(color)
			.setNormal(entry, -1, 1, 0).setLineWidth(2);
		
		// left
		buffer.addVertex(entry, x2, y1, z1).setColor(color)
			.setNormal(entry, 0, 1, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, 0, 1, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z2).setColor(color)
			.setNormal(entry, 0, 1, -1).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z1).setColor(color)
			.setNormal(entry, 0, 1, -1).setLineWidth(2);
		
		// front
		buffer.addVertex(entry, x2, y1, z2).setColor(color)
			.setNormal(entry, -1, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z2).setColor(color)
			.setNormal(entry, -1, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z2).setColor(color)
			.setNormal(entry, 1, 1, 0).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, 1, 1, 0).setLineWidth(2);
		
		// right
		buffer.addVertex(entry, x1, y1, z2).setColor(color)
			.setNormal(entry, 0, 1, -1).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z1).setColor(color)
			.setNormal(entry, 0, 1, -1).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, 0, 1, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z2).setColor(color)
			.setNormal(entry, 0, 1, 1).setLineWidth(2);
		
		// top
		buffer.addVertex(entry, x1, y2, z2).setColor(color)
			.setNormal(entry, 1, 0, -1).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z1).setColor(color)
			.setNormal(entry, 1, 0, -1).setLineWidth(2);
		buffer.addVertex(entry, x1, y2, z1).setColor(color)
			.setNormal(entry, 1, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y2, z2).setColor(color)
			.setNormal(entry, 1, 0, 1).setLineWidth(2);
		
		// bottom
		buffer.addVertex(entry, x2, y1, z1).setColor(color)
			.setNormal(entry, -1, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z2).setColor(color)
			.setNormal(entry, -1, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x1, y1, z1).setColor(color)
			.setNormal(entry, 1, 0, 1).setLineWidth(2);
		buffer.addVertex(entry, x2, y1, z2).setColor(color)
			.setNormal(entry, 1, 0, 1).setLineWidth(2);
	}
	
	public static void drawNode(PoseStack matrices, AABB box, int color,
		boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		drawNode(matrices, buffer, box.move(getCameraPos().reverse()), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawNodes(PoseStack matrices, List<AABB> boxes,
		int color, boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(AABB box : boxes)
			drawNode(matrices, buffer, box.move(camOffset), color);
		
		vcp.endBatch(layer);
	}
	
	public static void drawNodes(PoseStack matrices, List<ColoredBox> boxes,
		boolean depthTest)
	{
		MultiBufferSource.BufferSource vcp = getVCP();
		RenderType layer = WurstRenderLayers.getLines(depthTest);
		VertexConsumer buffer = vcp.getBuffer(layer);
		
		Vec3 camOffset = getCameraPos().reverse();
		for(ColoredBox box : boxes)
			drawNode(matrices, buffer, box.box().move(camOffset), box.color());
		
		vcp.endBatch(layer);
	}
	
	public static void drawNode(VertexConsumer buffer, AABB box, int color)
	{
		drawNode(new PoseStack(), buffer, box, color);
	}
	
	public static void drawNode(PoseStack matrices, VertexConsumer buffer,
		AABB box, int color)
	{
		PoseStack.Pose entry = matrices.last();
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
	
	public static void drawArrow(PoseStack matrices, VertexConsumer buffer,
		BlockPos from, BlockPos to, RegionPos region, int color)
	{
		Vec3 fromVec = from.getCenter().subtract(region.x(), 0, region.z());
		Vec3 toVec = to.getCenter().subtract(region.x(), 0, region.z());
		drawArrow(matrices, buffer, fromVec, toVec, color, 1 / 16F);
	}
	
	public static void drawArrow(VertexConsumer buffer, Vec3 from, Vec3 to,
		int color, float headSize)
	{
		drawArrow(new PoseStack(), buffer, from, to, color, headSize);
	}
	
	public static void drawArrow(PoseStack matrices, VertexConsumer buffer,
		Vec3 from, Vec3 to, int color, float headSize)
	{
		matrices.pushPose();
		PoseStack.Pose entry = matrices.last();
		Matrix4f matrix = entry.pose();
		
		// main line
		drawLine(matrices, buffer, from, to, color);
		
		matrices.translate(to);
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
		
		matrices.popPose();
	}
	
	public static void drawItem(GuiGraphics context, ItemStack stack, int x,
		int y, boolean large)
	{
		Matrix3x2fStack matrixStack = context.pose();
		
		matrixStack.pushMatrix();
		matrixStack.translate(x, y);
		if(large)
			matrixStack.scale(1.5F, 1.5F);
		else
			matrixStack.scale(0.75F, 0.75F);
		
		ItemStack renderStack = stack.isEmpty() || stack.getItem() == null
			? new ItemStack(Blocks.GRASS_BLOCK) : stack;
		
		context.renderItem(renderStack, 0, 0);
		
		matrixStack.popMatrix();
		
		if(stack.isEmpty())
		{
			context.guiRenderState.up();
			matrixStack.pushMatrix();
			matrixStack.translate(x, y);
			if(large)
				matrixStack.scale(2, 2);
			
			Font tr = WurstClient.MC.font;
			context.drawString(tr, "?", 3, 2, WurstColors.VERY_LIGHT_GRAY,
				true);
			
			matrixStack.popMatrix();
		}
	}
	
	/**
	 * Similar to {@link GuiGraphics#fill(int, int, int, int, int)}, but uses
	 * floating-point coordinates instead of integers.
	 */
	public static void fill2D(GuiGraphics context, float x1, float y1, float x2,
		float y2, int color)
	{
		int scale = WurstClient.MC.getWindow().getGuiScale();
		int xs1 = (int)(x1 * scale);
		int ys1 = (int)(y1 * scale);
		int xs2 = (int)(x2 * scale);
		int ys2 = (int)(y2 * scale);
		
		context.pose().pushMatrix();
		context.pose().scale(1F / scale);
		context.fill(xs1, ys1, xs2, ys2, color);
		context.pose().popMatrix();
	}
	
	/**
	 * Renders the given vertices in QUADS draw mode.
	 */
	public static void fillQuads2D(GuiGraphics context, float[][] vertices,
		int color)
	{
		Matrix3x2f pose = new Matrix3x2f(context.pose());
		ScreenRectangle scissor = context.scissorStack.peek();
		
		for(int i = 0; i < vertices.length - 3; i += 4)
		{
			if(i + 3 >= vertices.length)
				break;
			
			float x1 = vertices[i][0];
			float y1 = vertices[i][1];
			float x2 = vertices[i + 1][0];
			float y2 = vertices[i + 1][1];
			float x3 = vertices[i + 2][0];
			float y3 = vertices[i + 2][1];
			float x4 = vertices[i + 3][0];
			float y4 = vertices[i + 3][1];
			
			context.guiRenderState.submitGuiElement(new CustomQuadRenderState(
				pose, x1, y1, x2, y2, x3, y3, x4, y4, color, scissor));
		}
	}
	
	/**
	 * Pretends to render the given vertices in TRIANGLES draw mode
	 * by squeezing a bunch of quads into triangle shapes.
	 *
	 * <p>
	 * ...blame Vibrant Visuals.
	 */
	public static void fillTriangle2D(GuiGraphics context, float[][] vertices,
		int color)
	{
		Matrix3x2f pose = new Matrix3x2f(context.pose());
		ScreenRectangle scissor = context.scissorStack.peek();
		
		for(int i = 0; i < vertices.length - 2; i += 3)
		{
			if(i + 2 >= vertices.length)
				break;
			
			float x1 = vertices[i][0];
			float y1 = vertices[i][1];
			float x2 = vertices[i + 1][0];
			float y2 = vertices[i + 1][1];
			float x3 = vertices[i + 2][0];
			float y3 = vertices[i + 2][1];
			
			context.guiRenderState.submitGuiElement(new CustomQuadRenderState(
				pose, x1, y1, x2, y2, x3, y3, x3, y3, color, scissor));
		}
	}
	
	/**
	 * Similar to {@link GuiGraphics#hLine(int, int, int, int)} and
	 * {@link GuiGraphics#vLine(int, int, int, int)}, but supports
	 * diagonal lines, uses floating-point coordinates instead of integers, and
	 * is one actual pixel wide instead of one scaled pixel.
	 */
	public static void drawLine2D(GuiGraphics context, float x1, float y1,
		float x2, float y2, int color)
	{
		int scale = WurstClient.MC.getWindow().getGuiScale();
		float x = x1 * scale;
		float y = y1 * scale;
		float w = (x2 - x1) * scale;
		float h = (y2 - y1) * scale;
		float angle = (float)Mth.atan2(h, w);
		int length = Math.round(Mth.sqrt(w * w + h * h));
		
		context.pose().pushMatrix();
		context.pose().scale(1F / scale);
		context.pose().translate(x, y);
		context.pose().rotate(angle);
		context.pose().translate(-0.5F, -0.5F);
		context.hLine(0, length - 1, 0, color);
		context.pose().popMatrix();
	}
	
	/**
	 * Similar to {@link GuiGraphics#drawBorder(int, int, int, int, int)}, but
	 * uses floating-point coordinates instead of integers, and is one actual
	 * pixel wide instead of one scaled pixel.
	 */
	public static void drawBorder2D(GuiGraphics context, float x1, float y1,
		float x2, float y2, int color)
	{
		int scale = WurstClient.MC.getWindow().getGuiScale();
		int x = (int)(x1 * scale);
		int y = (int)(y1 * scale);
		int w = (int)((x2 - x1) * scale);
		int h = (int)((y2 - y1) * scale);
		
		context.pose().pushMatrix();
		context.pose().scale(1F / scale);
		context.hLine(x, x + w - 1, y, color);
		context.hLine(x, x + w - 1, y + h - 1, color);
		context.vLine(x, y + 1, y + h - 1, color);
		context.vLine(x + w - 1, y + 1, y + h - 1, color);
		context.pose().popMatrix();
	}
	
	/**
	 * Draws a 1px border around the given polygon.
	 */
	public static void drawLineStrip2D(GuiGraphics context, float[][] vertices,
		int color)
	{
		if(vertices.length < 2)
			return;
		
		for(int i = 1; i < vertices.length; i++)
			drawLine2D(context, vertices[i - 1][0], vertices[i - 1][1],
				vertices[i][0], vertices[i][1], color);
		drawLine2D(context, vertices[vertices.length - 1][0],
			vertices[vertices.length - 1][1], vertices[0][0], vertices[0][1],
			color);
	}
	
	/**
	 * Draws a box shadow around the given rectangle.
	 */
	public static void drawBoxShadow2D(GuiGraphics context, int x1, int y1,
		int x2, int y2)
	{
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// outline
		int outlineColor = toIntColor(acColor, 0.5F);
		drawBorder2D(context, x1, y1, x2, y2, outlineColor);
		
		// shadow
		float xs1 = x1 - 1;
		float xs2 = x2 + 1;
		float ys1 = y1 - 1;
		float ys2 = y2 + 1;
		
		int shadowColor1 = toIntColor(acColor, 0.75F);
		int shadowColor2 = 0x00000000;
		
		Matrix3x2f pose = new Matrix3x2f(context.pose());
		ScreenRectangle scissor = context.scissorStack.peek();
		
		// top
		context.guiRenderState.submitGuiElement(new CustomQuadRenderState(pose,
			x1, y1, x2, y1, xs2, ys1, xs1, ys1, shadowColor1, shadowColor1,
			shadowColor2, shadowColor2, scissor));
		
		// left
		context.guiRenderState.submitGuiElement(new CustomQuadRenderState(pose,
			xs1, ys1, xs1, ys2, x1, y2, x1, y1, shadowColor2, shadowColor2,
			shadowColor1, shadowColor1, scissor));
		
		// right
		context.guiRenderState.submitGuiElement(new CustomQuadRenderState(pose,
			x2, y1, x2, y2, xs2, ys2, xs2, ys1, shadowColor1, shadowColor1,
			shadowColor2, shadowColor2, scissor));
		
		// bottom
		context.guiRenderState.submitGuiElement(new CustomQuadRenderState(pose,
			x2, y2, x1, y2, xs1, ys2, xs2, ys2, shadowColor1, shadowColor1,
			shadowColor2, shadowColor2, scissor));
	}
	
	public record ColoredPoint(Vec3 point, int color)
	{}
	
	public record ColoredBox(AABB box, int color)
	{}
}
