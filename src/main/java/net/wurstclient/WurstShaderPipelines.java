/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;

import net.minecraft.client.gl.ShaderPipelines;
import net.minecraft.client.render.VertexFormats;

public enum WurstShaderPipelines
{
	;
	
	/**
	 * Similar to the DEBUG_LINE_STIP ShaderPipeline, but as a non-srip
	 * version with support for transparency.
	 */
	public static final RenderPipeline ONE_PIXEL_LINES = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56849)
			.withLocation("pipeline/wurst_1px_lines")
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withBlend(BlendFunction.TRANSLUCENT).withCull(false)
			.withVertexFormat(VertexFormats.field_1576, class_5596.DEBUG_LINES)
			.build());
	
	/**
	 * Similar to the DEBUG_LINE_STIP ShaderPipeline, but with support for
	 * transparency.
	 */
	public static final RenderPipeline ONE_PIXEL_LINE_STRIP = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56849)
			.withLocation("pipeline/wurst_1px_line_strip")
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withBlend(BlendFunction.TRANSLUCENT).withCull(false)
			.withVertexFormat(VertexFormats.field_1576,
				class_5596.DEBUG_LINE_STRIP)
			.build());
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no depth test.
	 */
	public static final RenderPipeline ESP_LINES = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56859)
			.withLocation("pipeline/wurst_esp_lines")
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
	
	/**
	 * Similar to the LINE_STRIP ShaderPipeline, but with no depth test.
	 */
	public static final RenderPipeline ESP_LINE_STRIP = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56859)
			.withLocation("pipeline/wurst_esp_line_strip")
			.withVertexFormat(VertexFormats.field_29337, class_5596.LINE_STRIP)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled.
	 */
	public static final RenderPipeline QUADS = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56860)
			.withLocation("pipeline/wurst_quads")
			.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
			.build());
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled
	 * and no depth test.
	 */
	public static final RenderPipeline ESP_QUADS = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56860)
			.withLocation("pipeline/wurst_esp_quads")
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
	
	/**
	 * Similar to the DEBUG_QUADS ShaderPipeline, but with no depth test.
	 */
	public static final RenderPipeline ESP_QUADS_NO_CULLING = ShaderPipelines
		.method_67887(RenderPipeline.builder(ShaderPipelines.field_56860)
			.withLocation("pipeline/wurst_esp_quads").withCull(false)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
}
