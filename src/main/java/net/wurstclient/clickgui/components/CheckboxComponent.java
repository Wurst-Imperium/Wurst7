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
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.settings.CheckboxSetting;

public final class CheckboxComponent extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
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
			case 0:
			setting.setChecked(!setting.isChecked());
			break;
			
			case 1:
			setting.setChecked(setting.isCheckedByDefault());
			break;
		}
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + 11;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		
		if(hovering && mouseX >= x3)
			setTooltip();
		
		if(setting.isLocked())
			hovering = false;
		
		drawBackground(x2, x3, y1, y2);
		drawBox(x1, x3, y1, y2, hovering);
		
		if(setting.isChecked())
			drawCheck(x1, y1, hovering);
		
		drawName(matrixStack, x3, y1);
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
			tooltip += "\n\nThis checkbox is locked to ";
			tooltip += setting.isChecked() + ".";
		}
		
		GUI.setTooltip(tooltip);
	}
	
	private void drawBackground(int x2, int x3, int y1, int y2)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
	}
	
	private void drawBox(int x1, int x3, int y1, int y2, boolean hovering)
	{
		float[] bgColor = GUI.getBgColor();
		float[] acColor = GUI.getAcColor();
		float opacity = GUI.getOpacity();
		
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
			hovering ? opacity * 1.5F : opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x3, y1);
		GL11.glEnd();
		
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x3, y1);
		GL11.glEnd();
	}
	
	private void drawCheck(int x1, int y1, boolean hovering)
	{
		double xc1 = x1 + 2.5;
		double xc2 = x1 + 3.5;
		double xc3 = x1 + 4.5;
		double xc4 = x1 + 7.5;
		double xc5 = x1 + 8.5;
		double yc1 = y1 + 2.5;
		double yc2 = y1 + 3.5;
		double yc3 = y1 + 5.5;
		double yc4 = y1 + 6.5;
		double yc5 = y1 + 8.5;
		
		// check
		if(setting.isLocked())
			GL11.glColor4f(0.5F, 0.5F, 0.5F, 0.75F);
		else
			GL11.glColor4f(0, hovering ? 1 : 0.85F, 0, 1);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2d(xc2, yc3);
		GL11.glVertex2d(xc3, yc4);
		GL11.glVertex2d(xc3, yc5);
		GL11.glVertex2d(xc1, yc4);
		GL11.glVertex2d(xc4, yc1);
		GL11.glVertex2d(xc5, yc2);
		GL11.glVertex2d(xc3, yc5);
		GL11.glVertex2d(xc3, yc4);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(xc2, yc3);
		GL11.glVertex2d(xc3, yc4);
		GL11.glVertex2d(xc4, yc1);
		GL11.glVertex2d(xc5, yc2);
		GL11.glVertex2d(xc3, yc5);
		GL11.glVertex2d(xc1, yc4);
		GL11.glEnd();
	}
	
	private void drawName(MatrixStack matrixStack, int x3, int y1)
	{
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		String name = setting.getName();
		int tx = x3 + 2;
		int ty = y1 + 2;
		int color = setting.isLocked() ? 0xAAAAAA : 0xF0F0F0;
		MC.textRenderer.draw(matrixStack, name, tx, ty, color);
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return MC.textRenderer.getWidth(setting.getName()) + 13;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
