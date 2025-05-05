/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.gl.VertexBuffer.Usage;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;

/**
 * An abstraction of Minecraft 1.21.5's new {@code GpuBuffer} system that makes
 * working with it as easy as {@code VertexBuffer} was.
 *
 * <p>
 * Backported to 1.21.4, where this is just a thin wrapper around
 * {@link VertexBuffer}.
 */
public final class EasyVertexBuffer implements AutoCloseable
{
	private final VertexBuffer vertexBuffer;
	
	/**
	 * Drop-in replacement for {@code VertexBuffer.createAndUpload()}.
	 */
	public static EasyVertexBuffer createAndUpload(DrawMode drawMode,
		VertexFormat format, Consumer<VertexConsumer> callback)
	{
		BufferBuilder bufferBuilder =
			Tessellator.getInstance().begin(drawMode, format);
		callback.accept(bufferBuilder);
		
		BuiltBuffer buffer = bufferBuilder.endNullable();
		if(buffer == null)
			return new EasyVertexBuffer();
		
		return new EasyVertexBuffer(buffer);
	}
	
	private EasyVertexBuffer(BuiltBuffer buffer)
	{
		vertexBuffer = new VertexBuffer(Usage.STATIC);
		vertexBuffer.bind();
		vertexBuffer.upload(buffer);
		VertexBuffer.unbind();
	}
	
	private EasyVertexBuffer()
	{
		vertexBuffer = null;
	}
	
	/**
	 * Similar to {@code VertexBuffer.draw(RenderLayer)}, but with a
	 * customizable view matrix. Use this if you need to translate/scale/rotate
	 * the buffer.
	 */
	public void draw(MatrixStack matrixStack, RenderLayer layer)
	{
		if(vertexBuffer == null)
			return;
		
		layer.startDrawing();
		vertexBuffer.bind();
		vertexBuffer.draw(matrixStack.peek().getPositionMatrix(),
			RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
		VertexBuffer.unbind();
		layer.endDrawing();
	}
	
	@Override
	public void close()
	{
		if(vertexBuffer != null)
			vertexBuffer.close();
	}
}
