/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

import org.joml.Matrix4fStack;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.BuiltBuffer.DrawParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

/**
 * An abstraction of Minecraft 1.21.5's new {@code GpuBuffer} system that makes
 * working with it as easy as {@code VertexBuffer} was.
 */
public final class EasyVertexBuffer implements AutoCloseable
{
	private final RenderSystem.ShapeIndexBuffer shapeIndexBuffer;
	private final GpuBuffer vertexBuffer;
	private final int indexCount;
	
	/**
	 * Drop-in replacement for {@code VertexBuffer.createAndUpload()}.
	 */
	public static EasyVertexBuffer createAndUpload(DrawMode drawMode,
		VertexFormat format, Consumer<VertexConsumer> callback)
	{
		BufferBuilder bufferBuilder =
			Tessellator.getInstance().method_60827(drawMode, format);
		callback.accept(bufferBuilder);
		
		try(BuiltBuffer buffer = bufferBuilder.endNullable())
		{
			if(buffer == null)
				return new EasyVertexBuffer(drawMode);
			
			return new EasyVertexBuffer(buffer, drawMode);
		}
	}
	
	private EasyVertexBuffer(BuiltBuffer buffer, DrawMode drawMode)
	{
		DrawParameters drawParams = buffer.getDrawParameters();
		shapeIndexBuffer = RenderSystem.getSequentialBuffer(drawParams.mode());
		indexCount = drawParams.indexCount();
		
		BufferType target = BufferType.VERTICES;
		BufferUsage usage = BufferUsage.STATIC_WRITE;
		vertexBuffer = RenderSystem.getDevice().createBuffer(null, target,
			usage, buffer.getBuffer());
	}
	
	private EasyVertexBuffer(DrawMode drawMode)
	{
		shapeIndexBuffer = null;
		indexCount = 0;
		vertexBuffer = null;
	}
	
	/**
	 * Similar to {@code VertexBuffer.draw(RenderLayer)}, but with a
	 * customizable view matrix. Use this if you need to translate/scale/rotate
	 * the buffer.
	 */
	public void draw(MatrixStack matrixStack, RenderLayer layer)
	{
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(matrixStack.peek().getPositionMatrix());
		
		draw(layer);
		
		modelViewStack.popMatrix();
	}
	
	/**
	 * Drop-in replacement for {@code VertexBuffer.draw(RenderLayer)}.
	 */
	public void draw(RenderLayer layer)
	{
		if(vertexBuffer == null)
			return;
		
		layer.startDrawing();
		Framebuffer framebuffer = layer.getTarget();
		RenderPipeline pipeline = layer.getPipeline();
		
		try(RenderPass renderPass =
			RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				framebuffer.getColorAttachment(), OptionalInt.empty(),
				framebuffer.useDepthAttachment
					? framebuffer.getDepthAttachment() : null,
				OptionalDouble.empty()))
		{
			renderPass.setPipeline(pipeline);
			renderPass.setVertexBuffer(0, vertexBuffer);
			renderPass.setIndexBuffer(
				shapeIndexBuffer.getIndexBuffer(indexCount),
				shapeIndexBuffer.getIndexType());
			renderPass.drawIndexed(0, indexCount);
		}
		
		layer.endDrawing();
	}
	
	@Override
	public void close()
	{
		if(vertexBuffer != null)
			vertexBuffer.close();
	}
}
