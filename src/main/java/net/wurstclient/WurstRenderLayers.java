/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.util.OptionalDouble;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderType#debugLineStrip(double)}, but as a
	 * non-srip version with support for transparency.
	 *
	 * @implNote Just like {@link RenderType#debugLineStrip(double)}, this
	 *           layer doesn't support any other line width than 1px. Changing
	 *           the line width number does nothing.
	 */
	public static final RenderType.CompositeRenderType ONE_PIXEL_LINES =
		RenderType.create("wurst:1px_lines", DefaultVertexFormat.POSITION_COLOR,
			Mode.DEBUG_LINES, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.POSITION_COLOR_SHADER)
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(1)))
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setCullState(RenderType.NO_CULL).createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugLineStrip(double)}, but with
	 * support for transparency.
	 *
	 * @implNote Just like {@link RenderType#debugLineStrip(double)}, this
	 *           layer doesn't support any other line width than 1px. Changing
	 *           the line width number does nothing.
	 */
	public static final RenderType.CompositeRenderType ONE_PIXEL_LINE_STRIP =
		RenderType.create("wurst:1px_line_strip",
			DefaultVertexFormat.POSITION_COLOR, Mode.DEBUG_LINE_STRIP, 1536,
			false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.POSITION_COLOR_SHADER)
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(1)))
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setCullState(RenderType.NO_CULL).createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lines()}, but with line width 2.
	 */
	public static final RenderType.CompositeRenderType LINES = RenderType
		.create("wurst:lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
			VertexFormat.Mode.LINES, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
				.setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
				.setCullState(RenderType.NO_CULL).createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lines()}, but with line width 2 and no
	 * depth test.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static final RenderType.CompositeRenderType ESP_LINES = RenderType
		.create("wurst:esp_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL,
			VertexFormat.Mode.LINES, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
				.setDepthTestState(RenderType.NO_DEPTH_TEST)
				.setCullState(RenderType.NO_CULL).createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lineStrip()}, but with line width 2.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static final RenderType.CompositeRenderType LINE_STRIP = RenderType
		.create("wurst:line_strip", DefaultVertexFormat.POSITION_COLOR_NORMAL,
			VertexFormat.Mode.LINE_STRIP, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
				.setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
				.setCullState(RenderType.NO_CULL).createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lineStrip()}, but with line width 2 and
	 * no depth test.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static final RenderType.CompositeRenderType ESP_LINE_STRIP =
		RenderType.create("wurst:esp_line_strip",
			DefaultVertexFormat.POSITION_COLOR_NORMAL,
			VertexFormat.Mode.LINE_STRIP, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.setWriteMaskState(RenderType.COLOR_DEPTH_WRITE)
				.setDepthTestState(RenderType.NO_DEPTH_TEST)
				.setCullState(RenderType.NO_CULL).createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugQuads()}, but with culling enabled.
	 */
	public static final RenderType.CompositeRenderType QUADS =
		RenderType.create("wurst:quads", DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.POSITION_COLOR_SHADER)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setDepthTestState(RenderType.LEQUAL_DEPTH_TEST)
				.createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugQuads()}, but with culling enabled
	 * and no depth test.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static final RenderType.CompositeRenderType ESP_QUADS =
		RenderType.create("wurst:esp_quads", DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS, 1536, false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.POSITION_COLOR_SHADER)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setDepthTestState(RenderType.NO_DEPTH_TEST)
				.createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugQuads()}, but with no depth test.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static final RenderType.CompositeRenderType ESP_QUADS_NO_CULLING =
		RenderType.create("wurst:esp_quads_no_culling",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536,
			false, true,
			RenderType.CompositeState.builder()
				.setShaderState(RenderType.POSITION_COLOR_SHADER)
				.setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
				.setCullState(RenderType.NO_CULL)
				.setDepthTestState(RenderType.NO_DEPTH_TEST)
				.createCompositeState(false));
	
	/**
	 * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
	 * value of {@code depthTest}.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static RenderType getQuads(boolean depthTest)
	{
		return depthTest ? QUADS : ESP_QUADS;
	}
	
	/**
	 * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
	 * value of {@code depthTest}.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static RenderType getLines(boolean depthTest)
	{
		return depthTest ? LINES : ESP_LINES;
	}
	
	/**
	 * Returns either {@link #LINE_STRIP} or {@link #ESP_LINE_STRIP} depending
	 * on the value of {@code depthTest}.
	 *
	 * @apiNote Until 25w08a (1.21.5), turning off depth test has to be done
	 *          manually, by calling
	 *          {@code RenderSystem.depthFunc(GlConst.GL_ALWAYS);} before
	 *          drawing the ESP lines. Without this code, ESP lines will be
	 *          drawn with depth test set to LEQUALS (only visible if not
	 *          obstructed).
	 */
	public static RenderType getLineStrip(boolean depthTest)
	{
		return depthTest ? LINE_STRIP : ESP_LINE_STRIP;
	}
}
