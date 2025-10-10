/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.render.RenderLayer;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2.
	 */
	public static final RenderLayer.MultiPhase LINES = RenderLayer.of(
		"wurst:lines", 1536, WurstShaderPipelines.DEPTH_TEST_LINES,
		RenderLayer.MultiPhaseParameters.builder()
			.layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
			.target(RenderLayer.ITEM_ENTITY_TARGET).build(false));
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderLayer.MultiPhase ESP_LINES =
		RenderLayer.of("wurst:esp_lines", 1536, WurstShaderPipelines.ESP_LINES,
			RenderLayer.MultiPhaseParameters.builder()
				.layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
				.target(RenderLayer.ITEM_ENTITY_TARGET).build(false));
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with culling enabled.
	 */
	public static final RenderLayer.MultiPhase QUADS = RenderLayer.of(
		"wurst:quads", 1536, false, true, WurstShaderPipelines.QUADS,
		RenderLayer.MultiPhaseParameters.builder().build(false));
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderLayer.MultiPhase ESP_QUADS = RenderLayer.of(
		"wurst:esp_quads", 1536, false, true, WurstShaderPipelines.ESP_QUADS,
		RenderLayer.MultiPhaseParameters.builder().build(false));
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with no depth test.
	 */
	public static final RenderLayer.MultiPhase ESP_QUADS_NO_CULLING =
		RenderLayer.of("wurst:esp_quads_no_culling", 1536, false, true,
			WurstShaderPipelines.ESP_QUADS_NO_CULLING,
			RenderLayer.MultiPhaseParameters.builder().build(false));
	
	/**
	 * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderLayer.MultiPhase getQuads(boolean depthTest)
	{
		return depthTest ? QUADS : ESP_QUADS;
	}
	
	/**
	 * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderLayer.MultiPhase getLines(boolean depthTest)
	{
		return depthTest ? LINES : ESP_LINES;
	}
}
