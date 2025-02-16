/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.util.OptionalDouble;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;

public enum WurstRenderLayers
{
	;
	
	/**
	 * Similar to {@link RenderLayer#getDebugLineStrip(double)}, but as a
	 * non-srip version with support for transparency.
	 *
	 * @implNote Just like {@link RenderLayer#getDebugLineStrip(double)}, this
	 *           layer doesn't support any other line width than 1px. Changing
	 *           the line width number does nothing.
	 */
	public static final RenderLayer.MultiPhase ONE_PIXEL_LINES =
		RenderLayer.of("wurst:1px_lines", VertexFormats.POSITION_COLOR,
			DrawMode.DEBUG_LINES, 1536, false, true,
			RenderLayer.MultiPhaseParameters.builder()
				.program(RenderLayer.POSITION_COLOR_PROGRAM)
				.lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1)))
				.transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
				.cull(RenderLayer.DISABLE_CULLING).build(false));
	
	/**
	 * Similar to {@link RenderLayer#getDebugLineStrip(double)}, but with
	 * support for transparency.
	 *
	 * @implNote Just like {@link RenderLayer#getDebugLineStrip(double)}, this
	 *           layer doesn't support any other line width than 1px. Changing
	 *           the line width number does nothing.
	 */
	public static final RenderLayer.MultiPhase ONE_PIXEL_LINE_STRIP =
		RenderLayer.of("wurst:1px_line_strip", VertexFormats.POSITION_COLOR,
			DrawMode.DEBUG_LINE_STRIP, 1536, false, true,
			RenderLayer.MultiPhaseParameters.builder()
				.program(RenderLayer.POSITION_COLOR_PROGRAM)
				.lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1)))
				.transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
				.cull(RenderLayer.DISABLE_CULLING).build(false));
	
	/**
	 * Similar to {@link RenderLayer#getLines()}, but with line width 2 and no
	 * depth test.
	 */
	public static final RenderLayer.MultiPhase ESP_LINES =
		RenderLayer.of("wurst:esp_lines", VertexFormats.LINES,
			VertexFormat.DrawMode.LINES, 1536, false, true,
			RenderLayer.MultiPhaseParameters.builder()
				.program(RenderLayer.LINES_PROGRAM)
				.lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2)))
				.layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
				.transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
				.target(RenderLayer.ITEM_ENTITY_TARGET)
				.writeMaskState(RenderLayer.ALL_MASK)
				.depthTest(RenderLayer.ALWAYS_DEPTH_TEST)
				.cull(RenderLayer.DISABLE_CULLING).build(false));
}
