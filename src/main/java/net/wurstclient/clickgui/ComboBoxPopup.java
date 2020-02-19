/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.EnumSetting;

public final class ComboBoxPopup<T extends Enum<T>> extends Popup
{
	private final ClickGui gui = WurstClient.INSTANCE.getGui();
	private final TextRenderer tr = WurstClient.MC.textRenderer;
	
	private final EnumSetting<T> setting;
	private final int popupWidth;
	
	public ComboBoxPopup(Component owner, EnumSetting<T> setting,
		int popupWidth)
	{
		super(owner);
		this.setting = setting;
		this.popupWidth = popupWidth;
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
		
		setX(owner.getWidth() - getWidth());
		setY(owner.getHeight());
	}
	
	@Override
	public void handleMouseClick(int mouseX, int mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		int yi1 = getY() - 11;
		for(T value : setting.getValues())
		{
			if(value == setting.getSelected())
				continue;
			
			yi1 += 11;
			int yi2 = yi1 + 11;
			
			if(mouseY < yi1 || mouseY >= yi2)
				continue;
			
			setting.setSelected(value);
			close();
			break;
		}
	}
	
	@Override
	public void render(int mouseX, int mouseY)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		
		if(hovering)
			gui.setTooltip("");
		
		drawOutline(x1, x2, y1, y2);
		
		int yi1 = y1 - 11;
		for(T value : setting.getValues())
		{
			if(value == setting.getSelected())
				continue;
			
			yi1 += 11;
			int yi2 = yi1 + 11;
			
			boolean hValue = hovering && mouseY >= yi1 && mouseY < yi2;
			drawValueBackground(x1, x2, yi1, yi2, hValue);
			
			drawValueName(x1, yi1, value);
		}
	}
	
	private boolean isHovering(int mouseX, int mouseY, int x1, int x2, int y1,
		int y2)
	{
		return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2;
	}
	
	private void drawOutline(int x1, int x2, int y1, int y2)
	{
		float[] acColor = gui.getAcColor();
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
	}
	
	private void drawValueBackground(int x1, int x2, int yi1, int yi2,
		boolean hValue)
	{
		float[] bgColor = gui.getBgColor();
		float alpha = gui.getOpacity() * (hValue ? 1.5F : 1);
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], alpha);
		
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, yi1);
		GL11.glVertex2i(x1, yi2);
		GL11.glVertex2i(x2, yi2);
		GL11.glVertex2i(x2, yi1);
		GL11.glEnd();
	}
	
	private void drawValueName(int x1, int yi1, Enum<?> value)
	{
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		tr.draw(value.toString(), x1 + 2, yi1 + 2, 0xF0F0F0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return popupWidth + 15;
	}
	
	@Override
	public int getDefaultHeight()
	{
		int numValues = setting.getValues().length;
		return (numValues - 1) * 11;
	}
}
