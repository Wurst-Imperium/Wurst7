/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;

public record CustomQuadRenderState(RenderPipeline pipeline,
	TextureSetup textureSetup, Matrix3x2f pose, float x1, float y1, float x2,
	float y2, float x3, float y3, float x4, float y4, int color1, int color2,
	int color3, int color4, @Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds) implements GuiElementRenderState
{
	public CustomQuadRenderState(Matrix3x2f pose, float x1, float y1, float x2,
		float y2, float x3, float y3, float x4, float y4, int color1,
		int color2, int color3, int color4,
		@Nullable ScreenRectangle scissorArea)
	{
		this(RenderPipelines.GUI, TextureSetup.noTexture(), pose, x1, y1, x2,
			y2, x3, y3, x4, y4, color1, color2, color3, color4, scissorArea,
			createBounds(x1, y1, x2, y2, x3, y3, x4, y4, pose, scissorArea));
	}
	
	public CustomQuadRenderState(Matrix3x2f pose, float x1, float y1, float x2,
		float y2, float x3, float y3, float x4, float y4, int color,
		@Nullable ScreenRectangle scissorArea)
	{
		this(pose, x1, y1, x2, y2, x3, y3, x4, y4, color, color, color, color,
			scissorArea);
	}
	
	@Override
	public void buildVertices(VertexConsumer vertices)
	{
		vertices.addVertexWith2DPose(pose(), x1(), y1()).setColor(color1());
		vertices.addVertexWith2DPose(pose(), x2(), y2()).setColor(color2());
		vertices.addVertexWith2DPose(pose(), x3(), y3()).setColor(color3());
		vertices.addVertexWith2DPose(pose(), x4(), y4()).setColor(color4());
	}
	
	@Nullable
	private static ScreenRectangle createBounds(float x1, float y1, float x2,
		float y2, float x3, float y3, float x4, float y4, Matrix3x2f pose,
		@Nullable ScreenRectangle scissorArea)
	{
		float minX = Math.min(x1, Math.min(x2, Math.min(x3, x4)));
		float maxX = Math.max(x1, Math.max(x2, Math.max(x3, x4)));
		float minY = Math.min(y1, Math.min(y2, Math.min(y3, y4)));
		float maxY = Math.max(y1, Math.max(y2, Math.max(y3, y4)));
		
		ScreenRectangle screenRect = new ScreenRectangle((int)minX, (int)minY,
			(int)(maxX - minX), (int)(maxY - minY)).transformMaxBounds(pose);
		return scissorArea != null ? scissorArea.intersection(screenRect)
			: screenRect;
	}
}
