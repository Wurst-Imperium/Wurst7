/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.newchunks;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.RenderUtils;

public final class NewChunksRenderer
{
	private final VertexBuffer[] vertexBuffers = new VertexBuffer[4];
	
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
	
	public void updateBuffer(int i, BuiltBuffer buffer)
	{
		vertexBuffers[i] = new VertexBuffer(VertexBuffer.Usage.STATIC);
		vertexBuffers[i].bind();
		vertexBuffers[i].upload(buffer);
		VertexBuffer.unbind();
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
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		Matrix4f projMatrix = RenderSystem.getProjectionMatrix();
		ShaderProgram shader = RenderSystem.getShader();
		
		float alpha = opacity.getValueF();
		float[] newColorF = newChunksColor.getColorF();
		float[] oldColorF = oldChunksColor.getColorF();
		double altitudeD = altitude.getValue();
		
		for(int i = 0; i < vertexBuffers.length; i++)
		{
			VertexBuffer buffer = vertexBuffers[i];
			if(buffer == null)
				continue;
			
			matrixStack.push();
			if(i == 0 || i == 2)
				matrixStack.translate(0, altitudeD, 0);
			
			if(i < 2)
				RenderSystem.setShaderColor(newColorF[0], newColorF[1],
					newColorF[2], alpha);
			else
				RenderSystem.setShaderColor(oldColorF[0], oldColorF[1],
					oldColorF[2], alpha);
			
			Matrix4f viewMatrix = matrixStack.peek().getPositionMatrix();
			buffer.bind();
			buffer.draw(viewMatrix, projMatrix, shader);
			VertexBuffer.unbind();
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
