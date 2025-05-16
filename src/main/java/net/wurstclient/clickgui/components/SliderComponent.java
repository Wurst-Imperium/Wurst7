/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.EditSliderScreen;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.RenderUtils;

public final class SliderComponent extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	private static final int TEXT_HEIGHT = 11;
	
	private final SliderSetting setting;
	private boolean dragging;
	
	public SliderComponent(SliderSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseY < getY() + 11)
			return;
		
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			if(Screen.hasControlDown())
				MC.setScreen(new EditSliderScreen(MC.currentScreen, setting));
			else
				dragging = true;
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			setting.setValue(setting.getDefaultValue());
			break;
		}
	}
	
	private void handleDragging(int mouseX, int x3, int x4)
	{
		if(!dragging)
			return;
		
		if(!GUI.isLeftMouseButtonPressed())
		{
			dragging = false;
			return;
		}
		
		double sliderStartX = x3;
		double sliderWidth = x4 - x3;
		double mousePercentage = (mouseX - sliderStartX) / sliderWidth;
		
		double min = setting.getMinimum();
		double range = setting.getRange();
		double value = min + range * mousePercentage;
		
		setting.setValue(value);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + 2;
		int x4 = x2 - 2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y1 + TEXT_HEIGHT;
		int y4 = y3 + 4;
		int y5 = y2 - 4;
		
		handleDragging(mouseX, x3, x4);
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseY < y3;
		boolean hSlider = hovering && mouseY >= y3 || dragging;
		
		boolean grayedOut = setting.isDisabled() || setting.isLocked();
		float opacity = GUI.getOpacity();
		float railOpacity = opacity * (hSlider ? 1.5F : 1);
		
		if(hText)
			GUI.setTooltip(getTextTooltip());
		else if(hSlider && !dragging)
			GUI.setTooltip(getSliderTooltip());
		
		if(grayedOut)
		{
			hovering = false;
			hSlider = false;
		}
		
		// background (around the rail)
		int bgColor = RenderUtils.toIntColor(GUI.getBgColor(), opacity);
		RenderUtils.fill2D(context, x1, y1, x2, y4, bgColor);
		RenderUtils.fill2D(context, x1, y5, x2, y2, bgColor);
		RenderUtils.fill2D(context, x1, y4, x3, y5, bgColor);
		RenderUtils.fill2D(context, x4, y4, x2, y5, bgColor);
		
		// limit
		float xl1 = x3;
		float xl2 = x4;
		if(!grayedOut && setting.isLimited())
		{
			double ratio = (x4 - x3) / setting.getRange();
			xl1 += ratio * (setting.getUsableMin() - setting.getMinimum());
			xl2 += ratio * (setting.getUsableMax() - setting.getMaximum());
			
			int limitColor =
				RenderUtils.toIntColor(new float[]{1, 0, 0}, railOpacity);
			RenderUtils.fill2D(context, x3, y4, xl1, y5, limitColor);
			RenderUtils.fill2D(context, xl2, y4, x4, y5, limitColor);
		}
		
		// rail
		RenderUtils.fill2D(context, xl1, y4, xl2, y5,
			RenderUtils.toIntColor(GUI.getBgColor(), railOpacity));
		RenderUtils.drawBorder2D(context, x3, y4, x4, y5,
			RenderUtils.toIntColor(GUI.getAcColor(), 0.5F));
		
		context.state.goUpLayer();
		
		// knob
		float xk1 = x1 + (x2 - x1 - 8) * (float)setting.getPercentage();
		float xk2 = xk1 + 8;
		float yk1 = y3 + 1.5F;
		float yk2 = y2 - 1.5F;
		int knobColor = grayedOut ? 0xC0808080 : RenderUtils
			.toIntColor(setting.getKnobColor(), hSlider ? 1 : 0.75F);
		RenderUtils.fill2D(context, xk1, yk1, xk2, yk2, knobColor);
		RenderUtils.drawBorder2D(context, xk1, yk1, xk2, yk2, 0x80101010);
		
		// text
		String name = setting.getName();
		String value = setting.getValueString();
		int valueWidth = TR.getWidth(value);
		int txtColor = GUI.getTxtColor();
		context.drawText(TR, name, x1, y1 + 2, txtColor, false);
		context.drawText(TR, value, x2 - valueWidth, y1 + 2, txtColor, false);
		
		context.state.goDownLayer();
	}
	
	private String getTextTooltip()
	{
		String tooltip = setting.getWrappedDescription(200);
		
		if(setting.isDisabled())
			tooltip += "\n\nThis slider is disabled.";
		else if(setting.isLocked())
		{
			tooltip += "\n\nThis slider is locked to ";
			tooltip += setting.getValueString() + ".";
		}
		
		return tooltip;
	}
	
	private String getSliderTooltip()
	{
		String tooltip =
			"\u00a7e[ctrl]\u00a7r+\u00a7e[left-click]\u00a7r for precise input\n";
		tooltip += "\u00a7e[right-click]\u00a7r to reset";
		return tooltip;
	}
	
	@Override
	public int getDefaultWidth()
	{
		int nameWitdh = TR.getWidth(setting.getName());
		int valueWidth = TR.getWidth(setting.getValueString());
		return nameWitdh + valueWidth + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return TEXT_HEIGHT * 2;
	}
}
