/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Arrays;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.ClickGuiIcons;
import net.wurstclient.clickgui.ComboBoxPopup;
import net.wurstclient.clickgui.Component;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.RenderUtils;

public final class ComboBoxComponent<T extends Enum<T>> extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	private static final int ARROW_SIZE = 11;
	
	private final EnumSetting<T> setting;
	private final int popupWidth;
	
	private ComboBoxPopup<T> popup;
	
	public ComboBoxComponent(EnumSetting<T> setting)
	{
		this.setting = setting;
		popupWidth = Arrays.stream(setting.getValues()).map(T::toString)
			.mapToInt(s -> TR.getWidth(s)).max().getAsInt();
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton,
		Click context)
	{
		if(mouseX < getX() + getWidth() - popupWidth - ARROW_SIZE - 4)
			return;
		
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			handleLeftClick();
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			handleRightClick();
			break;
		}
	}
	
	private void handleLeftClick()
	{
		if(isPopupOpen())
		{
			popup.close();
			popup = null;
			return;
		}
		
		popup = new ComboBoxPopup<>(this, setting, popupWidth);
		GUI.addPopup(popup);
	}
	
	private void handleRightClick()
	{
		if(isPopupOpen())
			return;
		
		setting.setSelected(setting.getDefaultSelected());
	}
	
	private boolean isPopupOpen()
	{
		return popup != null && !popup.isClosing();
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - ARROW_SIZE;
		int x4 = x3 - popupWidth - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseX < x4;
		boolean hBox = hovering && mouseX >= x4;
		
		// tooltip
		if(hText)
			GUI.setTooltip(setting.getWrappedDescription(200));
		
		// background
		context.fill(x1, y1, x4, y2, getFillColor(false));
		
		// box
		context.fill(x4, y1, x2, y2, getFillColor(hBox));
		
		context.state.goUpLayer();
		
		// outlines
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x4, y1, x2, y2, outlineColor);
		RenderUtils.drawLine2D(context, x3, y1, x3, y2, outlineColor);
		
		// arrow
		ClickGuiIcons.drawMinimizeArrow(context, x3, y1 + 0.5F, x2, y2 - 0.5F,
			hBox, !isPopupOpen());
		
		// text
		String name = setting.getName();
		String value = "" + setting.getSelected();
		int txtColor = GUI.getTxtColor();
		context.drawText(TR, name, x1, y1 + 2, txtColor, false);
		context.drawText(TR, value, x4 + 2, y1 + 2, txtColor, false);
	}
	
	private int getFillColor(boolean hovering)
	{
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(GUI.getBgColor(), opacity);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return TR.getWidth(setting.getName()) + popupWidth + ARROW_SIZE + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return ARROW_SIZE;
	}
}
