/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2.
	 */
	public static final RenderLayer LINES = RenderLayer.of("wurst:lines",
		RenderSetup.builder(WurstShaderPipelines.DEPTH_TEST_LINES)
			.layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.outputTarget(OutputTarget.ITEM_ENTITY_TARGET).build());
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderLayer ESP_LINES =
		RenderLayer.of("wurst:esp_lines",
			RenderSetup.builder(WurstShaderPipelines.ESP_LINES)
				.layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				.outputTarget(OutputTarget.ITEM_ENTITY_TARGET).build());
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with culling enabled.
	 */
	public static final RenderLayer QUADS = RenderLayer.of("wurst:quads",
		RenderSetup.builder(WurstShaderPipelines.QUADS).translucent().build());
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderLayer ESP_QUADS =
		RenderLayer.of("wurst:esp_quads", RenderSetup
			.builder(WurstShaderPipelines.ESP_QUADS).translucent().build());
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with no depth test.
	 */
	public static final RenderLayer ESP_QUADS_NO_CULLING =
		RenderLayer.of("wurst:esp_quads_no_culling",
			RenderSetup.builder(WurstShaderPipelines.ESP_QUADS_NO_CULLING)
				.translucent().useLightmap().build());
	
	/**
	 * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderLayer getQuads(boolean depthTest)
	{
		return depthTest ? QUADS : ESP_QUADS;
	}
	
	/**
	 * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderLayer getLines(boolean depthTest)
	{
		return depthTest ? LINES : ESP_LINES;
	}
}
