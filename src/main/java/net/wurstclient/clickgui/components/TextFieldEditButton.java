/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Objects;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.EditTextFieldScreen;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

public final class TextFieldEditButton extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	private static final int TEXT_HEIGHT = 11;
	
	private final TextFieldSetting setting;
	
	public TextFieldEditButton(TextFieldSetting setting)
	{
		this.setting = Objects.requireNonNull(setting);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseY < getY() + TEXT_HEIGHT)
			return;
		
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			MC.setScreen(new EditTextFieldScreen(MC.currentScreen, setting));
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			setting.resetToDefault();
			break;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y1 + TEXT_HEIGHT;
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseY < y3;
		boolean hBox = hovering && mouseY >= y3;
		
		if(hText)
			GUI.setTooltip(ChatUtils.wrapText(setting.getDescription(), 200));
		else if(hBox)
			GUI.setTooltip(ChatUtils.wrapText(setting.getValue(), 200));
		
		// background
		context.fill(x1, y1, x2, y3, RenderUtils.toIntColor(bgColor, opacity));
		
		// box
		context.fill(x1, y3, x2, y2,
			RenderUtils.toIntColor(bgColor, opacity * (hBox ? 1.5F : 1)));
		RenderUtils.drawBorder2D(context, x1, y3, x2, y2,
			RenderUtils.toIntColor(GUI.getAcColor(), 0.5F));
		
		// text
		int txtColor = GUI.getTxtColor();
		context.state.goUpLayer();
		context.drawText(TR, setting.getName(), x1, y1 + 2, txtColor, false);
		String value = setting.getValue();
		int maxWidth = getWidth() - TR.getWidth("...") - 2;
		int maxLength = TR.getTextHandler().getLimitedStringLength(value,
			maxWidth, Style.EMPTY);
		if(maxLength < value.length())
			value = value.substring(0, maxLength) + "...";
		context.drawText(TR, value, x1 + 2, y3 + 2, txtColor, false);
		context.state.goDownLayer();
	}
	
	@Override
	public int getDefaultWidth()
	{
		return TR.getWidth(setting.getName()) + 4;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return TEXT_HEIGHT * 2;
	}
}
