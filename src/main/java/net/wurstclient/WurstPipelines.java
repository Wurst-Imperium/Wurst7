/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import net.minecraft.class_10784;
import net.minecraft.class_10785;
import net.minecraft.class_10798;
import net.minecraft.class_10799;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public enum WurstPipelines
{
	;
	
	/**
	 * Similar to the DEBUG_LINE_STIP pipeline (class_10799.field_56836), but as
	 * a non-srip version with support for transparency.
	 */
	public static final class_10785 ONE_PIXEL_LINES = class_10799
		.method_67887(class_10785.method_67729(class_10799.field_56849)
			.method_67748("pipeline/wurst_1px_lines")
			.method_67762("core/position_color")
			.method_67757("core/position_color")
			.method_67744(class_10784.field_56701).method_67753(false)
			.method_67746(VertexFormats.POSITION_COLOR,
				VertexFormat.DrawMode.DEBUG_LINES)
			.method_67760());
	
	/**
	 * Similar to the DEBUG_LINE_STIP pipeline (class_10799.field_56836), but
	 * with support for transparency.
	 */
	public static final class_10785 ONE_PIXEL_LINE_STRIP = class_10799
		.method_67887(class_10785.method_67729(class_10799.field_56849)
			.method_67748("pipeline/wurst_1px_line_strip")
			.method_67762("core/position_color")
			.method_67757("core/position_color")
			.method_67744(class_10784.field_56701).method_67753(false)
			.method_67746(VertexFormats.POSITION_COLOR,
				VertexFormat.DrawMode.DEBUG_LINE_STRIP)
			.method_67760());
	
	/**
	 * Similar to the LINES pipeline (class_10799.field_56833), but with no
	 * depth test.
	 */
	public static final class_10785 ESP_LINES = class_10799
		.method_67887(class_10785.method_67729(class_10799.field_56859)
			.method_67748("pipeline/wurst_esp_lines")
			.method_67747(class_10798.NO_DEPTH_TEST).method_67760());
}
