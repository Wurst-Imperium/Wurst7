/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;

/**
 * An abstraction of Minecraft 1.21.5's new {@code GpuBuffer} system that makes
 * working with it as easy as {@code VertexBuffer} was.
 */
public final class EasyVertexBuffer implements AutoCloseable
{
	private final RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer;
	private final GpuBuffer vertexBuffer;
	private final int indexCount;
	
	/**
	 * Drop-in replacement for {@code VertexBuffer.createAndUpload()}.
	 */
	public static EasyVertexBuffer createAndUpload(Mode drawMode,
		VertexFormat format, Consumer<VertexConsumer> callback)
	{
		BufferBuilder bufferBuilder =
			Tesselator.getInstance().begin(drawMode, format);
		callback.accept(bufferBuilder);
		
		try(MeshData buffer = bufferBuilder.build())
		{
			if(buffer == null)
				return new EasyVertexBuffer(drawMode);
			
			return new EasyVertexBuffer(buffer, drawMode);
		}
	}
	
	private EasyVertexBuffer(MeshData buffer, Mode drawMode)
	{
		DrawState drawParams = buffer.drawState();
		shapeIndexBuffer = RenderSystem.getSequentialBuffer(drawParams.mode());
		indexCount = drawParams.indexCount();
		
		vertexBuffer = RenderSystem.getDevice().createBuffer(null,
			GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
			buffer.vertexBuffer());
	}
	
	private EasyVertexBuffer(Mode drawMode)
	{
		shapeIndexBuffer = null;
		indexCount = 0;
		vertexBuffer = null;
	}
	
	public void draw(PoseStack matrixStack, RenderType layer)
	{
		draw(matrixStack, layer, 1, 1, 1, 1);
	}
	
	public void draw(PoseStack matrixStack, RenderType layer, int argb)
	{
		float alpha = (argb >> 24 & 0xFF) / 255F;
		float red = (argb >> 16 & 0xFF) / 255F;
		float green = (argb >> 8 & 0xFF) / 255F;
		float blue = (argb & 0xFF) / 255F;
		draw(matrixStack, layer, red, green, blue, alpha);
	}
	
	public void draw(PoseStack matrixStack, RenderType layer, float[] rgba)
	{
		draw(matrixStack, layer, rgba[0], rgba[1], rgba[2], rgba[3]);
	}
	
	public void draw(PoseStack matrixStack, RenderType layer, float[] rgb,
		float alpha)
	{
		draw(matrixStack, layer, rgb[0], rgb[1], rgb[2], alpha);
	}
	
	/*
	 * Similar to {@link RenderLayer#draw(BuiltBuffer)}.
	 */
	public void draw(PoseStack matrixStack, RenderType layer, float red,
		float green, float blue, float alpha)
	{
		if(vertexBuffer == null)
			return;
		
		Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushMatrix();
		modelViewStack.mul(matrixStack.last().pose());
		
		GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrix(),
				new Vector4f(red, green, blue, alpha), new Vector3f(),
				TextureTransform.DEFAULT_TEXTURING.getMatrix());
		
		RenderTarget framebuffer =
			OutputTarget.ITEM_ENTITY_TARGET.getRenderTarget();
		RenderPipeline pipeline = layer.pipeline();
		GpuBuffer indexBuffer = shapeIndexBuffer.getBuffer(indexCount);
		
		try(RenderPass renderPass =
			RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "something from Wurst", framebuffer.getColorTextureView(),
				OptionalInt.empty(), framebuffer.getDepthTextureView(),
				OptionalDouble.empty()))
		{
			renderPass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
			renderPass.setVertexBuffer(0, vertexBuffer);
			renderPass.setIndexBuffer(indexBuffer, shapeIndexBuffer.type());
			renderPass.drawIndexed(0, 0, indexCount, 1);
		}
		
		modelViewStack.popMatrix();
	}
	
	@Override
	public void close()
	{
		if(vertexBuffer != null)
			vertexBuffer.close();
	}
}
