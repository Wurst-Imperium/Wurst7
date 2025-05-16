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

import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;

public record FakeTriangleRenderState(RenderPipeline pipeline,
	TextureSetup textureSetup, Matrix3x2f pose, float x1, float y1, float x2,
	float y2, float x3, float y3, int color, @Nullable ScreenRect scissorArea,
	@Nullable ScreenRect bounds) implements SimpleGuiElementRenderState
{
	public FakeTriangleRenderState(RenderPipeline pipeline,
		TextureSetup textureSetup, Matrix3x2f pose, float x1, float y1,
		float x2, float y2, float x3, float y3, int color,
		@Nullable ScreenRect scissorArea)
	{
		this(pipeline, textureSetup, pose, x1, y1, x2, y2, x3, y3, color,
			scissorArea,
			createBounds(x1, y1, x2, y2, x3, y3, pose, scissorArea));
	}
	
	@Override
	public void setupVertices(VertexConsumer vertices, float depth)
	{
		// Real triangles don't work for some reason,
		// so we build one out of a quad instead.
		
		vertices.vertex(pose(), x1(), y1(), depth).color(color());
		vertices.vertex(pose(), x2(), y2(), depth).color(color());
		vertices.vertex(pose(), x3(), y3(), depth).color(color());
		vertices.vertex(pose(), x3(), y3(), depth).color(color());
	}
	
	@Nullable
	private static ScreenRect createBounds(float x1, float y1, float x2,
		float y2, float x3, float y3, Matrix3x2f pose,
		@Nullable ScreenRect scissorArea)
	{
		float minX = Math.min(x1, Math.min(x2, x3));
		float maxX = Math.max(x1, Math.max(x2, x3));
		float minY = Math.min(y1, Math.min(y2, y3));
		float maxY = Math.max(y1, Math.max(y2, y3));
		
		ScreenRect screenRect = new ScreenRect((int)minX, (int)minY,
			(int)(maxX - minX), (int)(maxY - minY)).transformEachVertex(pose);
		return scissorArea != null ? scissorArea.intersection(screenRect)
			: screenRect;
	}
}
