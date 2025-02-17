/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.client.gl.BlendPrograms;
import net.minecraft.client.gl.ShaderProgramLayer;
import net.minecraft.client.gl.ShaderProgramLayers;
import net.minecraft.client.render.DepthTestState;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public enum WurstShaderLayers
{
	;
	
	/**
	 * Similar to the DEBUG_LINE_STIP ShaderProgramLayer, but as a non-srip
	 * version with support for transparency.
	 */
	public static final ShaderProgramLayer ONE_PIXEL_LINES =
		ShaderProgramLayers.addProgram(ShaderProgramLayer
			.create(ShaderProgramLayers.MATRICIES_COLOR)
			.setName("pipeline/wurst_1px_lines")
			.setVertex("core/position_color").setShard("core/position_color")
			.addBlend(BlendPrograms.TRANSLUCENT).addCulling(false)
			.addFormat(VertexFormats.POSITION_COLOR,
				VertexFormat.DrawMode.DEBUG_LINES)
			.create());
	
	/**
	 * Similar to the DEBUG_LINE_STIP ShaderProgramLayer, but with support for
	 * transparency.
	 */
	public static final ShaderProgramLayer ONE_PIXEL_LINE_STRIP =
		ShaderProgramLayers.addProgram(ShaderProgramLayer
			.create(ShaderProgramLayers.MATRICIES_COLOR)
			.setName("pipeline/wurst_1px_line_strip")
			.setVertex("core/position_color").setShard("core/position_color")
			.addBlend(BlendPrograms.TRANSLUCENT).addCulling(false)
			.addFormat(VertexFormats.POSITION_COLOR,
				VertexFormat.DrawMode.DEBUG_LINE_STRIP)
			.create());
	
	/**
	 * Similar to the LINES ShaderProgramLayer, but with no depth test.
	 */
	public static final ShaderProgramLayer ESP_LINES =
		ShaderProgramLayers.addProgram(
			ShaderProgramLayer.create(ShaderProgramLayers.RENDERTYPE_LINES)
				.setName("pipeline/wurst_esp_lines")
				.AddDepthProgram(DepthTestState.NO_DEPTH_TEST).create());
	
	/**
	 * Similar to the LINE_STRIP ShaderProgramLayer, but with no depth test.
	 */
	public static final ShaderProgramLayer ESP_LINE_STRIP =
		ShaderProgramLayers.addProgram(
			ShaderProgramLayer.create(ShaderProgramLayers.RENDERTYPE_LINES)
				.setName("pipeline/wurst_esp_line_strip")
				.addFormat(VertexFormats.LINES, DrawMode.LINE_STRIP)
				.AddDepthProgram(DepthTestState.NO_DEPTH_TEST).create());
	
	/**
	 * Similar to the DEBUG_QUADS ShaderProgramLayer, but with culling enabled
	 * and no depth test.
	 */
	public static final ShaderProgramLayer ESP_QUADS =
		ShaderProgramLayers.addProgram(
			ShaderProgramLayer.create(ShaderProgramLayers.POSITION_COLOR)
				.setName("pipeline/wurst_esp_quads")
				.AddDepthProgram(DepthTestState.NO_DEPTH_TEST).create());
}
