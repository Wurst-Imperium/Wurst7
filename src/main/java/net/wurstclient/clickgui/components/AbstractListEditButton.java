/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.RenderUtils;

public abstract class AbstractListEditButton extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final Font TR = MC.font;
	
	private final String buttonText = "Edit...";
	private final int buttonWidth = TR.width(buttonText);
	
	protected abstract void openScreen();
	
	protected abstract String getText();
	
	protected abstract Setting getSetting();
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton,
		MouseButtonEvent context)
	{
		if(mouseButton != GLFW.GLFW_MOUSE_BUTTON_LEFT)
			return;
		
		if(mouseX < getX() + getWidth() - buttonWidth - 4)
			return;
		
		openScreen();
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - buttonWidth - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseX < x3;
		boolean hBox = hovering && mouseX >= x3;
		
		if(hText)
			GUI.setTooltip(getSetting().getWrappedDescription(200));
		
		// background
		context.fill(x1, y1, x3, y2, getFillColor(false));
		
		// button
		context.fill(x3, y1, x2, y2, getFillColor(hBox));
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x3, y1, x2, y2, outlineColor);
		
		// text
		int txtColor = GUI.getTxtColor();
		context.guiRenderState.up();
		context.drawString(TR, getText(), x1, y1 + 2, txtColor, false);
		context.drawString(TR, buttonText, x3 + 2, y1 + 2, txtColor, false);
	}
	
	private int getFillColor(boolean hovering)
	{
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(GUI.getBgColor(), opacity);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return TR.width(getText()) + buttonWidth + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
