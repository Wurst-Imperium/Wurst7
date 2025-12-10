/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderType#getLines()}, but with line width 2.
	 */
	public static final RenderType LINES = RenderType.create("wurst:lines",
		RenderSetup.builder(WurstShaderPipelines.DEPTH_TEST_LINES)
			.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
			.createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getLines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderType ESP_LINES =
		RenderType.create("wurst:esp_lines",
			RenderSetup.builder(WurstShaderPipelines.ESP_LINES)
				.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
				.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
				.createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getDebugQuads()}, but with culling enabled.
	 */
	public static final RenderType QUADS = RenderType.create("wurst:quads",
		RenderSetup.builder(WurstShaderPipelines.QUADS).sortOnUpload()
			.createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getDebugQuads()}, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderType ESP_QUADS = RenderType.create(
		"wurst:esp_quads", RenderSetup.builder(WurstShaderPipelines.ESP_QUADS)
			.sortOnUpload().createRenderSetup());
	
	/**
	 * Similar to {@link RenderType#getDebugQuads()}, but with no depth test.
	 */
	public static final RenderType ESP_QUADS_NO_CULLING =
		RenderType.create("wurst:esp_quads_no_culling",
			RenderSetup.builder(WurstShaderPipelines.ESP_QUADS_NO_CULLING)
				.sortOnUpload().useLightmap().createRenderSetup());
	
	/**
	 * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderType getQuads(boolean depthTest)
	{
		return depthTest ? QUADS : ESP_QUADS;
	}
	
	/**
	 * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
	 * value of {@code depthTest}.
	 */
	public static RenderType getLines(boolean depthTest)
	{
		return depthTest ? LINES : ESP_LINES;
	}
}
