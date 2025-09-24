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
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.EditColorScreen;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.ColorUtils;
import net.wurstclient.util.RenderUtils;

public final class ColorComponent extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	private static final int TEXT_HEIGHT = 11;
	
	private final ColorSetting setting;
	
	public ColorComponent(ColorSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton,
		Click context)
	{
		if(mouseY < getY() + TEXT_HEIGHT)
			return;
		
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			MC.setScreen(new EditColorScreen(MC.currentScreen, setting));
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			setting.setColor(setting.getDefaultColor());
			break;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y1 + TEXT_HEIGHT;
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseY < y3;
		boolean hColor = hovering && mouseY >= y3;
		
		if(hText)
			GUI.setTooltip(setting.getWrappedDescription(200));
		else if(hColor)
			GUI.setTooltip(getColorTooltip());
		
		// background
		float opacity = GUI.getOpacity();
		int bgColor = RenderUtils.toIntColor(GUI.getBgColor(), opacity);
		context.fill(x1, y1, x2, y3, bgColor);
		
		// box
		context.fill(x1, y3, x2, y2,
			setting.getColorI(hovering ? 1F : opacity));
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x1, y3, x2, y2, outlineColor);
		
		// text
		String name = setting.getName();
		String value = ColorUtils.toHex(setting.getColor());
		int valueWidth = TR.getWidth(value);
		int txtColor = GUI.getTxtColor();
		context.state.goUpLayer();
		context.drawText(TR, name, x1, y1 + 2, txtColor, false);
		context.drawText(TR, value, x2 - valueWidth, y1 + 2, txtColor, false);
	}
	
	private String getColorTooltip()
	{
		String tooltip = "\u00a7cR:\u00a7r" + setting.getRed();
		tooltip += " \u00a7aG:\u00a7r" + setting.getGreen();
		tooltip += " \u00a79B:\u00a7r" + setting.getBlue();
		tooltip += "\n\n\u00a7e[left-click]\u00a7r to edit";
		tooltip += "\n\u00a7e[right-click]\u00a7r to reset";
		return tooltip;
	}
	
	@Override
	public int getDefaultWidth()
	{
		return TR.getWidth(setting.getName() + "#FFFFFF") + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return TEXT_HEIGHT * 2;
	}
}
