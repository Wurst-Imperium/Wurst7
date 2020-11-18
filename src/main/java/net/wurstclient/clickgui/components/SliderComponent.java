/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.screens.EditSliderScreen;
import net.wurstclient.settings.SliderSetting;

public final class SliderComponent extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
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
			case 0:
			handleLeftClick();
			break;
			
			case 1:
			handleRightClick();
			break;
		}
	}
	
	private void handleLeftClick()
	{
		if(Screen.hasControlDown())
			MC.openScreen(new EditSliderScreen(MC.currentScreen, setting));
		else
			dragging = true;
	}
	
	private void handleRightClick()
	{
		setting.setValue(setting.getDefaultValue());
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + 2;
		int x4 = x2 - 2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y1 + 11;
		int y4 = y3 + 4;
		int y5 = y2 - 4;
		
		handleDragging(mouseX, x3, x4);
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		boolean hSlider = hovering && mouseY >= y3 || dragging;
		boolean renderAsDisabled = setting.isDisabled() || setting.isLocked();
		
		if(hovering && mouseY < y3)
			setTooltip();
		else if(hSlider && !dragging)
			GUI.setTooltip(
				"\u00a7e[ctrl]\u00a7r+\u00a7e[left-click]\u00a7r for precise input");
		
		if(renderAsDisabled)
		{
			hovering = false;
			hSlider = false;
		}
		
		drawBackground(x1, x2, x3, x4, y1, y2, y4, y5);
		drawRail(x3, x4, y4, y5, hSlider, renderAsDisabled);
		drawKnob(x1, x2, y2, y3, hSlider, renderAsDisabled);
		drawNameAndValue(matrixStack, x1, x2, y1, renderAsDisabled);
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
	
	private boolean isHovering(int mouseX, int mouseY, int x1, int x2, int y1,
		int y2)
	{
		Window parent = getParent();
		boolean scrollEnabled = parent.isScrollingEnabled();
		int scroll = scrollEnabled ? parent.getScrollOffset() : 0;
		
		return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2
			&& mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
	}
	
	private void setTooltip()
	{
		String tooltip = setting.getDescription();
		
		if(setting.isLocked())
		{
			tooltip += "\n\nThis slider is locked to ";
			tooltip += setting.getValueString() + ".";
			
		}else if(setting.isDisabled())
			tooltip += "\n\nThis slider is disabled.";
		
		GUI.setTooltip(tooltip);
	}
	
	private void drawBackground(int x1, int x2, int x3, int x4, int y1, int y2,
		int y4, int y5)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y4);
		GL11.glVertex2i(x2, y4);
		GL11.glVertex2i(x2, y1);
		
		GL11.glVertex2i(x1, y5);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y5);
		
		GL11.glVertex2i(x1, y4);
		GL11.glVertex2i(x1, y5);
		GL11.glVertex2i(x3, y5);
		GL11.glVertex2i(x3, y4);
		
		GL11.glVertex2i(x4, y4);
		GL11.glVertex2i(x4, y5);
		GL11.glVertex2i(x2, y5);
		GL11.glVertex2i(x2, y4);
		
		GL11.glEnd();
	}
	
	private void drawRail(int x3, int x4, int y4, int y5, boolean hSlider,
		boolean renderAsDisabled)
	{
		float[] bgColor = GUI.getBgColor();
		float[] acColor = GUI.getAcColor();
		float opacity = GUI.getOpacity();
		
		double xl1 = x3;
		double xl2 = x4;
		if(!renderAsDisabled && setting.isLimited())
		{
			double ratio = (x4 - x3) / setting.getRange();
			xl1 += ratio * (setting.getUsableMin() - setting.getMinimum());
			xl2 += ratio * (setting.getUsableMax() - setting.getMaximum());
		}
		
		// limit
		GL11.glColor4f(1, 0, 0, hSlider ? opacity * 1.5F : opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2d(x3, y4);
		GL11.glVertex2d(x3, y5);
		GL11.glVertex2d(xl1, y5);
		GL11.glVertex2d(xl1, y4);
		GL11.glVertex2d(xl2, y4);
		GL11.glVertex2d(xl2, y5);
		GL11.glVertex2d(x4, y5);
		GL11.glVertex2d(x4, y4);
		GL11.glEnd();
		
		// background
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
			hSlider ? opacity * 1.5F : opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2d(xl1, y4);
		GL11.glVertex2d(xl1, y5);
		GL11.glVertex2d(xl2, y5);
		GL11.glVertex2d(xl2, y4);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x3, y4);
		GL11.glVertex2i(x3, y5);
		GL11.glVertex2i(x4, y5);
		GL11.glVertex2i(x4, y4);
		GL11.glEnd();
	}
	
	private void drawKnob(int x1, int x2, int y2, int y3, boolean hSlider,
		boolean renderAsDisabled)
	{
		double percentage = (setting.getValue() - setting.getMinimum())
			/ (setting.getMaximum() - setting.getMinimum());
		double xk1 = x1 + (x2 - x1 - 8) * percentage;
		double xk2 = xk1 + 8;
		double yk1 = y3 + 1.5;
		double yk2 = y2 - 1.5;
		
		// knob
		if(renderAsDisabled)
			GL11.glColor4f(0.5F, 0.5F, 0.5F, 0.75F);
		else
		{
			float f = (float)(2 * percentage);
			GL11.glColor4f(f, 2 - f, 0, hSlider ? 1 : 0.75F);
		}
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2d(xk1, yk1);
		GL11.glVertex2d(xk1, yk2);
		GL11.glVertex2d(xk2, yk2);
		GL11.glVertex2d(xk2, yk1);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(xk1, yk1);
		GL11.glVertex2d(xk1, yk2);
		GL11.glVertex2d(xk2, yk2);
		GL11.glVertex2d(xk2, yk1);
		GL11.glEnd();
	}
	
	private void drawNameAndValue(MatrixStack matrixStack, int x1, int x2,
		int y1, boolean renderAsDisabled)
	{
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		TextRenderer tr = MC.textRenderer;
		String name = setting.getName();
		String value = setting.getValueString();
		int valueWidth = tr.getWidth(value);
		int color = renderAsDisabled ? 0xAAAAAA : 0xF0F0F0;
		tr.draw(matrixStack, name, x1, y1 + 2, color);
		tr.draw(matrixStack, value, x2 - valueWidth, y1 + 2, color);
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer tr = MC.textRenderer;
		int nameWitdh = tr.getWidth(setting.getName());
		int valueWidth = tr.getWidth(setting.getValueString());
		return nameWitdh + valueWidth + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 22;
	}
}
