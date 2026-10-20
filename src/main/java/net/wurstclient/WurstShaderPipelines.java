/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;

import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

public enum WurstShaderPipelines
{
	;
	
	/**
	 * Similar to the RENDERTYPE_LINES Snippet, but without fog.
	 */
	public static final Snippet FOGLESS_LINES_SNIPPET = RenderPipeline.builder()
		.withBindGroupLayout(BindGroupLayouts.GLOBALS)
		.withBindGroupLayout(BindGroupLayouts.PROJECTION)
		.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
		.withVertexShader(Identifier.parse("wurst:core/fogless_lines"))
		.withFragmentShader(Identifier.parse("wurst:core/fogless_lines"))
		.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
		.withCull(false)
		.withVertexBinding(0,
			DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
		.withPrimitiveTopology(PrimitiveTopology.LINES).buildSnippet();
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no fog.
	 */
	public static final RenderPipeline DEPTH_TEST_LINES = RenderPipeline
		.builder(FOGLESS_LINES_SNIPPET)
		.withLocation(Identifier.parse("wurst:pipeline/wurst_depth_test_lines"))
		.withDepthStencilState(DepthStencilState.DEFAULT).build();
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no depth test or fog.
	 */
	public static final RenderPipeline ESP_LINES =
		RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
			.withLocation(Identifier.parse("wurst:pipeline/wurst_esp_lines"))
			.withDepthStencilState(DepthStencilState.OFF).build();
	
	private static final Snippet QUADS_SNIPPET = RenderPipeline.builder()
		.withBindGroupLayout(BindGroupLayouts.GLOBALS)
		.withBindGroupLayout(BindGroupLayouts.PROJECTION)
		.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
		.withVertexShader("core/position_color")
		.withFragmentShader("core/position_color")
		.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
		.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
		.withPrimitiveTopology(PrimitiveTopology.QUADS).buildSnippet();
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled.
	 */
	public static final RenderPipeline QUADS =
		RenderPipeline.builder(QUADS_SNIPPET)
			.withLocation(Identifier.parse("wurst:pipeline/wurst_quads"))
			.withDepthStencilState(DepthStencilState.DEFAULT).withCull(true)
			.build();
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderPipeline ESP_QUADS = RenderPipeline
		.builder(QUADS_SNIPPET)
		.withLocation(Identifier.parse("wurst:pipeline/wurst_esp_quads"))
		.withDepthStencilState(DepthStencilState.OFF).withCull(true).build();
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with no depth test.
	 */
	public static final RenderPipeline ESP_QUADS_NO_CULLING = RenderPipeline
		.builder(QUADS_SNIPPET)
		.withLocation(
			Identifier.parse("wurst:pipeline/wurst_esp_quads_no_culling"))
		.withDepthStencilState(DepthStencilState.OFF).withCull(false).build();
}
