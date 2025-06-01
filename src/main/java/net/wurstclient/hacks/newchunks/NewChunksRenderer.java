/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import java.util.function.Consumer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.BufferWithLayer;
import net.wurstclient.util.RenderUtils;

public final class NewChunksRenderer
{
	private final BufferWithLayer[] vertexBuffers = new BufferWithLayer[4];
	
	private final SliderSetting altitude;
	private final SliderSetting opacity;
	private final ColorSetting newChunksColor;
	private final ColorSetting oldChunksColor;
	
	public NewChunksRenderer(SliderSetting altitude, SliderSetting opacity,
		ColorSetting newChunksColor, ColorSetting oldChunksColor)
	{
		this.altitude = altitude;
		this.opacity = opacity;
		this.newChunksColor = newChunksColor;
		this.oldChunksColor = oldChunksColor;
	}
	
	public void updateBuffer(int i, RenderLayer.MultiPhase layer,
		Consumer<VertexConsumer> callback)
	{
		vertexBuffers[i] = BufferWithLayer.createAndUpload(layer, callback);
	}
	
	public void closeBuffers()
	{
		for(int i = 0; i < vertexBuffers.length; i++)
		{
			if(vertexBuffers[i] == null)
				continue;
			
			vertexBuffers[i].close();
			vertexBuffers[i] = null;
		}
	}
	
	public void render(MatrixStack matrixStack, float partialTicks)
	{
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		float alpha = opacity.getValueF();
		double altitudeD = altitude.getValue();
		
		for(int i = 0; i < vertexBuffers.length; i++)
		{
			BufferWithLayer buffer = vertexBuffers[i];
			if(buffer == null)
				continue;
			
			matrixStack.push();
			if(i == 0 || i == 2)
				matrixStack.translate(0, altitudeD, 0);
			
			float[] rgb =
				i < 2 ? newChunksColor.getColorF() : oldChunksColor.getColorF();
			
			buffer.draw(matrixStack, rgb, alpha);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
	}
}
