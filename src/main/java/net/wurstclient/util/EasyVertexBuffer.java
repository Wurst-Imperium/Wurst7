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

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlBufferTarget;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.GpuBuffer;
import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.BuiltBuffer.DrawParameters;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPass;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
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
			Tessellator.getInstance().begin(drawMode, format);
		callback.accept(bufferBuilder);
		
		try(BuiltBuffer buffer = bufferBuilder.endNullable())
		{
			return new EasyVertexBuffer(buffer, drawMode);
		}
	}
	
	private EasyVertexBuffer(BuiltBuffer buffer, DrawMode drawMode)
	{
		DrawParameters drawParams = buffer.getDrawParameters();
		shapeIndexBuffer = RenderSystem.getSequentialBuffer(drawParams.mode());
		indexCount = buffer == null ? 0 : drawParams.indexCount();
		
		GlBufferTarget target = GlBufferTarget.VERTICES;
		GlUsage usage = GlUsage.STATIC_WRITE;
		vertexBuffer = buffer == null ? null : RenderSystem.getDevice()
			.createBuffer(null, target, usage, buffer.getBuffer());
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
		Framebuffer framebuffer = layer.method_68494();
		ShaderPipeline pipeline = layer.method_68495();
		
		try(RenderPass renderPass =
			RenderSystem.getDevice().getResourceManager().newRenderPass(
				framebuffer.getColorAttachment(), OptionalInt.empty(),
				framebuffer.useDepthAttachment
					? framebuffer.getDepthAttachment() : null,
				OptionalDouble.empty()))
		{
			renderPass.bindShader(pipeline);
			renderPass.setVertexBuffer(0, vertexBuffer);
			renderPass.setIndexBuffer(shapeIndexBuffer.method_68274(indexCount),
				shapeIndexBuffer.getIndexType());
			renderPass.drawObjects(0, indexCount);
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
