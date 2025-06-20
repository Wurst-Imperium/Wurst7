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
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.SelectFileScreen;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.RenderUtils;

public final class FileComponent extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	
	private final FileSetting setting;
	
	public FileComponent(FileSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT)
			return;
		
		if(mouseX < getX() + getWidth() - getButtonWidth() - 4)
			return;
		
		MC.setScreen(new SelectFileScreen(MC.currentScreen, setting));
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - getButtonWidth() - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseX < x3;
		boolean hBox = hovering && mouseX >= x3;
		
		// tooltip
		if(hText)
			GUI.setTooltip(setting.getWrappedDescription(200));
		else if(hBox)
			GUI.setTooltip("\u00a7e[left-click]\u00a7r to select file");
		
		// background
		context.fill(x1, y1, x3, y2, getFillColor(false));
		
		// button
		context.fill(x3, y1, x2, y2, getFillColor(hBox));
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x3, y1, x2, y2, outlineColor);
		
		// text
		int txtColor = GUI.getTxtColor();
		String labelText = setting.getName() + ":";
		String buttonText = setting.getSelectedFileName();
		context.state.goUpLayer();
		context.drawText(TR, labelText, x1, y1 + 2, txtColor, false);
		context.drawText(TR, buttonText, x3 + 2, y1 + 2, txtColor, false);
		context.state.goDownLayer();
	}
	
	private int getFillColor(boolean hovering)
	{
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(GUI.getBgColor(), opacity);
	}
	
	private int getButtonWidth()
	{
		return TR.getWidth(setting.getSelectedFileName());
	}
	
	@Override
	public int getDefaultWidth()
	{
		String text = setting.getName() + ":";
		return TR.getWidth(text) + getButtonWidth() + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
