/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.class_12245;
import net.minecraft.class_12246;
import net.minecraft.class_12247;
import net.minecraft.client.render.RenderLayer;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2.
	 */
	public static final RenderLayer LINES =
		RenderLayer.method_75940("wurst:lines",
			class_12247.method_75927(WurstShaderPipelines.DEPTH_TEST_LINES)
				.method_75930(class_12245.VIEW_OFFSET_Z_LAYERING)
				.method_75931(class_12246.ITEM_ENTITY_TARGET).method_75938());
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderLayer ESP_LINES =
		RenderLayer.method_75940("wurst:esp_lines",
			class_12247.method_75927(WurstShaderPipelines.ESP_LINES)
				.method_75930(class_12245.VIEW_OFFSET_Z_LAYERING)
				.method_75931(class_12246.ITEM_ENTITY_TARGET).method_75938());
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with culling enabled.
	 */
	public static final RenderLayer QUADS = RenderLayer.method_75940(
		"wurst:quads", class_12247.method_75927(WurstShaderPipelines.QUADS)
			.method_75937().method_75938());
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderLayer ESP_QUADS =
		RenderLayer.method_75940("wurst:esp_quads",
			class_12247.method_75927(WurstShaderPipelines.ESP_QUADS)
				.method_75937().method_75938());
	
	/**
	 * Similar to {@link RenderLayer#getDebugQuads()}, but with no depth test.
	 */
	public static final RenderLayer ESP_QUADS_NO_CULLING =
		RenderLayer.method_75940("wurst:esp_quads_no_culling",
			class_12247.method_75927(WurstShaderPipelines.ESP_QUADS_NO_CULLING)
				.method_75937().method_75928().method_75938());
	
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
