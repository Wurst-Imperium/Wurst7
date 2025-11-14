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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.RenderType;

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
	public static EasyVertexBuffer createAndUpload(Mode drawMode,
		VertexFormat format, Consumer<VertexConsumer> callback)
	{
		BufferBuilder bufferBuilder =
			Tesselator.getInstance().begin(drawMode, format);
		callback.accept(bufferBuilder);
		
		MeshData buffer = bufferBuilder.build();
		if(buffer == null)
			return new EasyVertexBuffer();
		
		return new EasyVertexBuffer(buffer);
	}
	
	private EasyVertexBuffer(MeshData buffer)
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
	public void draw(PoseStack matrixStack, RenderType layer)
	{
		if(vertexBuffer == null)
			return;
		
		layer.setupRenderState();
		vertexBuffer.bind();
		vertexBuffer.drawWithShader(matrixStack.last().pose(),
			RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
		VertexBuffer.unbind();
		layer.clearRenderState();
	}
	
	@Override
	public void close()
	{
		if(vertexBuffer != null)
			vertexBuffer.close();
	}
}
