/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.function.Consumer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public record BufferWithLayer(EasyVertexBuffer buffer,
	RenderLayer.MultiPhase layer) implements AutoCloseable
{
	public static BufferWithLayer createAndUpload(RenderLayer.MultiPhase layer,
		Consumer<VertexConsumer> callback)
	{
		return new BufferWithLayer(EasyVertexBuffer.createAndUpload(
			layer.getDrawMode(), layer.getVertexFormat(), callback), layer);
	}
	
	public void draw(MatrixStack matrixStack)
	{
		buffer.draw(matrixStack, layer);
	}
	
	public void draw(MatrixStack matrixStack, float red, float green,
		float blue, float alpha)
	{
		buffer.draw(matrixStack, layer, red, green, blue, alpha);
	}
	
	public void draw(MatrixStack matrixStack, float[] rgba)
	{
		buffer.draw(matrixStack, layer, rgba);
	}
	
	public void draw(MatrixStack matrixStack, float[] rgb, float alpha)
	{
		buffer.draw(matrixStack, layer, rgb, alpha);
	}
	
	public void draw(MatrixStack matrixStack, int argb)
	{
		buffer.draw(matrixStack, layer, argb);
	}
	
	@Override
	public void close()
	{
		buffer.close();
	}
}
