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
import net.wurstclient.clickgui.ClickGuiIcons;
import net.wurstclient.clickgui.Component;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.RenderUtils;

public final class CheckboxComponent extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	private static final int BOX_SIZE = 11;
	
	private final CheckboxSetting setting;
	
	public CheckboxComponent(CheckboxSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			setting.setChecked(!setting.isChecked());
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			setting.setChecked(setting.isCheckedByDefault());
			break;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + BOX_SIZE;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseX >= x3;
		
		if(hText)
			GUI.setTooltip(getTooltip());
		
		if(setting.isLocked())
			hovering = false;
		
		// background
		context.fill(x3, y1, x2, y2, getFillColor(false));
		
		// box
		context.fill(x1, y1, x3, y2, getFillColor(hovering));
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x1, y1, x3, y2, outlineColor);
		
		context.state.goUpLayer();
		
		// check
		if(setting.isChecked())
			ClickGuiIcons.drawCheck(context, x1, y1, x3, y2, hovering,
				setting.isLocked());
		
		// text
		String name = setting.getName();
		context.drawText(TR, name, x3 + 2, y1 + 2, GUI.getTxtColor(), false);
		
		context.state.goDownLayer();
	}
	
	private int getFillColor(boolean hovering)
	{
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(GUI.getBgColor(), opacity);
	}
	
	private String getTooltip()
	{
		String tooltip = setting.getWrappedDescription(200);
		if(setting.isLocked())
		{
			tooltip += "\n\nThis checkbox is locked to ";
			tooltip += setting.isChecked() + ".";
		}
		
		return tooltip;
	}
	
	@Override
	public int getDefaultWidth()
	{
		return BOX_SIZE + TR.getWidth(setting.getName()) + 2;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return BOX_SIZE;
	}
}
