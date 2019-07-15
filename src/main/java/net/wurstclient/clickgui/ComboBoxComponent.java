/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import java.util.Arrays;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.EnumSetting;

public final class ComboBoxComponent extends Component
{
	private final EnumSetting setting;
	private final int popupWidth;
	private ComboBoxPopup popup;
	
	public ComboBoxComponent(EnumSetting setting)
	{
		this.setting = setting;
		
		TextRenderer fr = WurstClient.MC.textRenderer;
		popupWidth = Arrays.stream(setting.getValues())
			.mapToInt(v -> fr.getStringWidth(v.toString())).max().getAsInt();
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(int mouseX, int mouseY, int mouseButton)
	{
		if(mouseX < getX() + getWidth() - popupWidth - 15)
			return;
		
		if(mouseButton == 0)
		{
			if(popup != null && !popup.isClosing())
			{
				popup.close();
				popup = null;
				return;
			}
			
			popup = new ComboBoxPopup(this);
			ClickGui gui = WurstClient.INSTANCE.getGui();
			gui.addPopup(popup);
			
		}else if(mouseButton == 1 && (popup == null || popup.isClosing()))
			setting.setSelected(setting.getDefaultSelected().toString());
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - 11;
		int x4 = x3 - popupWidth - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		int scroll = getParent().isScrollingEnabled()
			? getParent().getScrollOffset() : 0;
		boolean hovering = mouseX >= x1 && mouseY >= y1 && mouseX < x2
			&& mouseY < y2 && mouseY >= -scroll
			&& mouseY < getParent().getHeight() - 13 - scroll;
		boolean hText = hovering && mouseX < x4;
		boolean hBox = hovering && mouseX >= x4;
		
		// tooltip
		if(hText)
			gui.setTooltip(setting.getDescription());
		
		// background
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x4, y2);
		GL11.glVertex2i(x4, y1);
		GL11.glEnd();
		
		// box
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
			hBox ? opacity * 1.5F : opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x4, y1);
		GL11.glVertex2i(x4, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x4, y1);
		GL11.glVertex2i(x4, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		// separator
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glEnd();
		
		double xa1 = x3 + 1;
		double xa2 = (x3 + x2) / 2.0;
		double xa3 = x2 - 1;
		double ya1;
		double ya2;
		
		if(popup != null && !popup.isClosing())
		{
			ya1 = y2 - 3.5;
			ya2 = y1 + 3;
			GL11.glColor4f(hBox ? 1 : 0.85F, 0, 0, 1);
		}else
		{
			ya1 = y1 + 3.5;
			ya2 = y2 - 3;
			GL11.glColor4f(0, hBox ? 1 : 0.85F, 0, 1);
		}
		
		// arrow
		GL11.glBegin(GL11.GL_TRIANGLES);
		GL11.glVertex2d(xa1, ya1);
		GL11.glVertex2d(xa3, ya1);
		GL11.glVertex2d(xa2, ya2);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(xa1, ya1);
		GL11.glVertex2d(xa3, ya1);
		GL11.glVertex2d(xa2, ya2);
		GL11.glEnd();
		
		// setting name
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TextRenderer fr = WurstClient.MC.textRenderer;
		fr.draw(setting.getName(), x1, y1 + 2, 0xf0f0f0);
		fr.draw(setting.getSelected().toString(), x4 + 2, y1 + 2, 0xf0f0f0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer fr = WurstClient.MC.textRenderer;
		return fr.getStringWidth(setting.getName()) + popupWidth + 17;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
	
	private static class ComboBoxPopup extends Popup
	{
		public ComboBoxPopup(ComboBoxComponent owner)
		{
			super(owner);
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
			
			Enum[] values = ((ComboBoxComponent)getOwner()).setting.getValues();
			int yi1 = getY() - 11;
			for(Enum value : values)
			{
				if(value == ((ComboBoxComponent)getOwner()).setting
					.getSelected())
					continue;
				
				yi1 += 11;
				int yi2 = yi1 + 11;
				if(mouseY < yi1 || mouseY >= yi2)
					continue;
				
				((ComboBoxComponent)getOwner()).setting
					.setSelected(value.toString());
				close();
				break;
			}
		}
		
		@Override
		public void render(int mouseX, int mouseY)
		{
			ClickGui gui = WurstClient.INSTANCE.getGui();
			float[] bgColor = gui.getBgColor();
			float[] acColor = gui.getAcColor();
			float opacity = gui.getOpacity();
			
			int x1 = getX();
			int x2 = x1 + getWidth();
			int y1 = getY();
			int y2 = y1 + getHeight();
			
			boolean hovering =
				mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2;
			if(hovering)
				gui.setTooltip(null);
			
			// outline
			GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2i(x1, y1);
			GL11.glVertex2i(x1, y2);
			GL11.glVertex2i(x2, y2);
			GL11.glVertex2i(x2, y1);
			GL11.glEnd();
			
			Enum[] values = ((ComboBoxComponent)getOwner()).setting.getValues();
			int yi1 = y1 - 11;
			for(Enum value : values)
			{
				if(value == ((ComboBoxComponent)getOwner()).setting
					.getSelected())
					continue;
				
				yi1 += 11;
				int yi2 = yi1 + 11;
				boolean hValue = hovering && mouseY >= yi1 && mouseY < yi2;
				
				// background
				GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
					hValue ? opacity * 1.5F : opacity);
				GL11.glBegin(GL11.GL_QUADS);
				GL11.glVertex2i(x1, yi1);
				GL11.glVertex2i(x1, yi2);
				GL11.glVertex2i(x2, yi2);
				GL11.glVertex2i(x2, yi1);
				GL11.glEnd();
				
				// value name
				GL11.glColor4f(1, 1, 1, 1);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				TextRenderer fr = WurstClient.MC.textRenderer;
				fr.draw(value.toString(), x1 + 2, yi1 + 2, 0xf0f0f0);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
			}
		}
		
		@Override
		public int getDefaultWidth()
		{
			return ((ComboBoxComponent)getOwner()).popupWidth + 15;
		}
		
		@Override
		public int getDefaultHeight()
		{
			return (((ComboBoxComponent)getOwner()).setting.getValues().length
				- 1) * 11;
		}
	}
}
