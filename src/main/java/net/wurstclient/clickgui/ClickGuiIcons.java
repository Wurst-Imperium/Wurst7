/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import net.minecraft.client.gui.DrawContext;
import net.wurstclient.WurstClient;
import net.wurstclient.util.RenderUtils;

public enum ClickGuiIcons
{
	;
	
	public static void drawMinimizeArrow(DrawContext context, float x1,
		float y1, float x2, float y2, boolean hovering, boolean minimized)
	{
		float xa1 = x1 + 1;
		float xa2 = (x1 + x2) / 2;
		float xa3 = x2 - 1;
		float ya1;
		float ya2;
		
		// arrow
		int arrowColor;
		float[][] arrowVertices;
		if(minimized)
		{
			ya1 = y1 + 3;
			ya2 = y2 - 2.5F;
			arrowColor = hovering ? 0xFF00FF00 : 0xFF00D900;
			arrowVertices = new float[][]{{xa1, ya1}, {xa2, ya2}, {xa3, ya1}};
			
		}else
		{
			ya1 = y2 - 3;
			ya2 = y1 + 2.5F;
			arrowColor = hovering ? 0xFFFF0000 : 0xFFD90000;
			arrowVertices = new float[][]{{xa1, ya1}, {xa3, ya1}, {xa2, ya2}};
		}
		RenderUtils.fillTriangle2D(context, arrowVertices, arrowColor);
		
		// outline
		int outlineColor = 0x80101010;
		RenderUtils.drawLineStrip2D(context, arrowVertices, outlineColor);
	}
	
	public static void drawRadarArrow(DrawContext context, float x1, float y1,
		float x2, float y2)
	{
		float x3 = x1 + (x2 - x1) / 2;
		float y3 = y1 + (y2 - y1) * 0.75F;
		
		// arrow
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int arrowColor =
			RenderUtils.toIntColor(gui.getAcColor(), gui.getOpacity());
		float[][] arrowVertices = {{x3, y1}, {x1, y2}, {x3, y3}, {x2, y2}};
		RenderUtils.fillQuads2D(context, arrowVertices, arrowColor);
		
		// outline
		int outlineColor = 0x80101010;
		RenderUtils.drawLineStrip2D(context, arrowVertices, outlineColor);
	}
	
	public static void drawPin(DrawContext context, float x1, float y1,
		float x2, float y2, boolean hovering, boolean pinned)
	{
		int needleColor = hovering ? 0xFFFFFFFF : 0xFFD9D9D9;
		int outlineColor = 0x80101010;
		
		if(pinned)
		{
			float xk1 = x1 + 2;
			float xk2 = x2 - 2;
			float xk3 = x1 + 1;
			float xk4 = x2 - 1;
			float yk1 = y1 + 2;
			float yk2 = y2 - 2;
			float yk3 = y2 - 0.5F;
			
			// knob
			int knobColor = hovering ? 0xFFFF0000 : 0xFFD90000;
			RenderUtils.fill2D(context, xk1, yk1, xk2, yk2, knobColor);
			RenderUtils.fill2D(context, xk3, yk2, xk4, yk3, knobColor);
			
			float xn1 = x1 + 3.5F;
			float xn2 = x2 - 3.5F;
			float yn1 = y2 - 0.5F;
			float yn2 = y2;
			
			// needle
			RenderUtils.fill2D(context, xn1, yn1, xn2, yn2, needleColor);
			
			// outlines
			RenderUtils.drawBorder2D(context, xk1, yk1, xk2, yk2, outlineColor);
			RenderUtils.drawBorder2D(context, xk3, yk2, xk4, yk3, outlineColor);
			RenderUtils.drawBorder2D(context, xn1, yn1, xn2, yn2, outlineColor);
			
		}else
		{
			float xk1 = x2 - 3.5F;
			float xk2 = x2 - 0.5F;
			float xk3 = x2 - 3;
			float xk4 = x1 + 3;
			float xk5 = x1 + 2;
			float xk6 = x2 - 2;
			float xk7 = x1 + 1;
			float yk1 = y1 + 0.5F;
			float yk2 = y1 + 3.5F;
			float yk3 = y2 - 3;
			float yk4 = y1 + 3;
			float yk5 = y1 + 2;
			float yk6 = y2 - 2;
			float yk7 = y2 - 1;
			
			// knob
			int knobColor = hovering ? 0xFF00FF00 : 0xFF00D900;
			float[][] knobVertices = {{xk4, yk4}, {xk3, yk3}, {xk2, yk2},
				{xk1, yk1}, {xk5, yk5}, {xk7, yk4}, {xk3, yk7}, {xk6, yk6}};
			RenderUtils.fillQuads2D(context, knobVertices, knobColor);
			
			float xn1 = x1 + 3;
			float xn2 = x1 + 4;
			float xn3 = x1 + 1;
			float yn1 = y2 - 4;
			float yn2 = y2 - 3;
			float yn3 = y2 - 1;
			
			// needle
			float[][] needleVertices = {{xn3, yn3}, {xn2, yn2}, {xn1, yn1}};
			RenderUtils.fillTriangle2D(context, needleVertices, needleColor);
			
			// outlines
			float[][] knobPart1 = new float[4][2];
			System.arraycopy(knobVertices, 0, knobPart1, 0, 4);
			RenderUtils.drawLineStrip2D(context, knobPart1, outlineColor);
			float[][] knobPart2 = new float[4][2];
			System.arraycopy(knobVertices, 4, knobPart2, 0, 4);
			RenderUtils.drawLineStrip2D(context, knobPart2, outlineColor);
			RenderUtils.drawLineStrip2D(context, needleVertices, outlineColor);
		}
	}
	
	public static void drawCheck(DrawContext context, float x1, float y1,
		float x2, float y2, boolean hovering, boolean grayedOut)
	{
		float xc1 = x1 + 2.5F;
		float xc2 = x1 + 3.5F;
		float xc3 = (x1 + x2) / 2 - 1;
		float xc4 = x2 - 3.5F;
		float xc5 = x2 - 2.5F;
		float yc1 = y1 + 2.5F;
		float yc2 = y1 + 3.5F;
		float yc3 = (y1 + y2) / 2;
		float yc4 = yc3 + 1;
		float yc5 = y2 - 4.5F;
		float yc6 = y2 - 2.5F;
		
		// check
		int checkColor =
			grayedOut ? 0xC0808080 : hovering ? 0xFF00FF00 : 0xFF00D900;
		float[][] checkVertices = {{xc2, yc3}, {xc1, yc4}, {xc3, yc6},
			{xc3, yc5}, {xc3, yc5}, {xc3, yc6}, {xc5, yc2}, {xc4, yc1}};
		RenderUtils.fillQuads2D(context, checkVertices, checkColor);
		
		// outline
		int outlineColor = 0x80101010;
		float[][] outlineVertices = {{xc2, yc3}, {xc3, yc5}, {xc4, yc1},
			{xc5, yc2}, {xc3, yc6}, {xc1, yc4}, {xc2, yc3}};
		RenderUtils.drawLineStrip2D(context, outlineVertices, outlineColor);
	}
	
	public static void drawIndeterminateCheck(DrawContext context, float x1,
		float y1, float x2, float y2, boolean hovering, boolean grayedOut)
	{
		float xc1 = x1 + 2.5F;
		float xc2 = x2 - 2.5F;
		float yc1 = y1 + 2.5F;
		float yc2 = y2 - 2.5F;
		
		// fill
		int checkColor =
			grayedOut ? 0xC0808080 : hovering ? 0xFF00FF00 : 0xFF00D900;
		RenderUtils.fill2D(context, xc1, yc1, xc2, yc2, checkColor);
		
		// outline
		int outlineColor = 0x80101010;
		RenderUtils.drawBorder2D(context, xc1, yc1, xc2, yc2, outlineColor);
	}
	
	public static void drawCross(DrawContext context, float x1, float y1,
		float x2, float y2, boolean hovering)
	{
		float xc1 = x1 + 2;
		float xc2 = x1 + 3;
		float xc3 = x2 - 2;
		float xc4 = x2 - 3;
		float xc5 = x1 + 3.5F;
		float xc6 = (x1 + x2) / 2;
		float xc7 = x2 - 3.5F;
		float yc1 = y1 + 3;
		float yc2 = y1 + 2;
		float yc3 = y2 - 3;
		float yc4 = y2 - 2;
		float yc5 = y1 + 3.5F;
		float yc6 = (y1 + y2) / 2;
		float yc7 = y2 - 3.5F;
		
		// cross
		int crossColor = hovering ? 0xFFFF0000 : 0xFFD90000;
		float[][] crossVertices = {{xc2, yc2}, {xc1, yc1}, {xc4, yc4},
			{xc3, yc3}, {xc3, yc1}, {xc4, yc2}, {xc6, yc5}, {xc7, yc6},
			{xc6, yc7}, {xc5, yc6}, {xc1, yc3}, {xc2, yc4}};
		RenderUtils.fillQuads2D(context, crossVertices, crossColor);
		
		// outline
		int outlineColor = 0x80101010;
		float[][] outlineVertices = {{xc1, yc1}, {xc2, yc2}, {xc6, yc5},
			{xc4, yc2}, {xc3, yc1}, {xc7, yc6}, {xc3, yc3}, {xc4, yc4},
			{xc6, yc7}, {xc2, yc4}, {xc1, yc3}, {xc5, yc6}};
		RenderUtils.drawLineStrip2D(context, outlineVertices, outlineColor);
	}
}
