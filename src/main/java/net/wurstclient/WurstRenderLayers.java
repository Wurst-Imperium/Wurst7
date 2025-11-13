/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.util.OptionalDouble;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderType#lines()}, but with line width 2.
	 */
	public static final RenderType.CompositeRenderType LINES = RenderType
		.create("wurst:lines", 1536, WurstShaderPipelines.DEPTH_TEST_LINES,
			RenderType.CompositeState.builder()
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderType.CompositeRenderType ESP_LINES = RenderType
		.create("wurst:esp_lines", 1536, WurstShaderPipelines.ESP_LINES,
			RenderType.CompositeState.builder()
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lineStrip()}, but with line width 2.
	 */
	public static final RenderType.CompositeRenderType LINE_STRIP =
		RenderType.create("wurst:line_strip", 1536, false, true,
			WurstShaderPipelines.DEPTH_TEST_LINE_STRIP,
			RenderType.CompositeState.builder()
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#lineStrip()}, but with line width 2 and
	 * no depth test.
	 */
	public static final RenderType.CompositeRenderType ESP_LINE_STRIP =
		RenderType.create("wurst:esp_line_strip", 1536, false, true,
			WurstShaderPipelines.ESP_LINE_STRIP,
			RenderType.CompositeState.builder()
				.setLineState(
					new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
				.setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
				.setOutputState(RenderType.ITEM_ENTITY_TARGET)
				.createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugQuads()}, but with culling enabled.
	 */
	public static final RenderType.CompositeRenderType QUADS = RenderType
		.create("wurst:quads", 1536, false, true, WurstShaderPipelines.QUADS,
			RenderType.CompositeState.builder().createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugQuads()}, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderType.CompositeRenderType ESP_QUADS =
		RenderType.create("wurst:esp_quads", 1536, false, true,
			WurstShaderPipelines.ESP_QUADS,
			RenderType.CompositeState.builder().createCompositeState(false));
	
	/**
	 * Similar to {@link RenderType#debugQuads()}, but with no depth test.
	 */
	public static final RenderType.CompositeRenderType ESP_QUADS_NO_CULLING =
		RenderType.create("wurst:esp_quads_no_culling", 1536, false, true,
			WurstShaderPipelines.ESP_QUADS_NO_CULLING,
			RenderType.CompositeState.builder().createCompositeState(false));
	
	/**
	 * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderType.CompositeRenderType getQuads(boolean depthTest)
	{
		return depthTest ? QUADS : ESP_QUADS;
	}
	
	/**
	 * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderType.CompositeRenderType getLines(boolean depthTest)
	{
		return depthTest ? LINES : ESP_LINES;
	}
	
	/**
	 * Returns either {@link #LINE_STRIP} or {@link #ESP_LINE_STRIP} depending
	 * on the value of {@code depthTest}.
	 */
	public static RenderType.CompositeRenderType getLineStrip(boolean depthTest)
	{
		return depthTest ? LINE_STRIP : ESP_LINE_STRIP;
	}
}
