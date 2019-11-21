/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Objects;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.SettingsWindow;
import net.wurstclient.clickgui.Window;

public final class FeatureButton extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
	private final Feature feature;
	private final boolean hasSettings;
	
	private Window settingsWindow;
	
	public FeatureButton(Feature feature)
	{
		this.feature = Objects.requireNonNull(feature);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
		hasSettings = !feature.getSettings().isEmpty();
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		if(hasSettings && (mouseX > getX() + getWidth() - 12
			|| feature.getPrimaryAction().isEmpty()))
		{
			if(isSettingsWindowOpen())
				closeSettingsWindow();
			else
				openSettingsWindow();
			
			return;
		}
		
		feature.doPrimaryAction();
	}
	
	private boolean isSettingsWindowOpen()
	{
		return settingsWindow != null && !settingsWindow.isClosing();
	}
	
	private void openSettingsWindow()
	{
		settingsWindow = new SettingsWindow(feature, getParent(), getY());
		GUI.addWindow(settingsWindow);
	}
	
	private void closeSettingsWindow()
	{
		settingsWindow.close();
		settingsWindow = null;
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = hasSettings ? x2 - 11 : x2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		boolean hHack = hovering && mouseX < x3;
		boolean hSettings = hovering && mouseX >= x3;
		
		if(hHack)
			setTooltip();
		
		drawButtonBackground(x1, x3, y1, y2, hHack);
		
		if(hasSettings)
			drawSettingsBackground(x2, x3, y1, y2, hSettings);
		
		drawOutline(x1, x2, y1, y2);
		
		if(hasSettings)
		{
			drawSeparator(x3, y1, y2);
			drawSettingsArrow(x2, x3, y1, y2, hSettings);
		}
		
		drawName(x1, x3, y1);
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
		String tooltip = feature.getDescription();
		
		// if(feature.isBlocked())
		// {
		// if(tooltip == null)
		// tooltip = "";
		// else
		// tooltip += "\n\n";
		// tooltip +=
		// "Your current YesCheat+ profile is blocking this feature.";
		// }
		
		GUI.setTooltip(tooltip);
	}
	
	private void drawButtonBackground(int x1, int x3, int y1, int y2,
		boolean hHack)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		GL11.glBegin(GL11.GL_QUADS);
		
		if(feature.isEnabled())
			// if(feature.isBlocked())
			// glColor4f(1, 0, 0, hHack ? opacity * 1.5F : opacity);
			// else
			GL11.glColor4f(0, 1, 0, hHack ? opacity * 1.5F : opacity);
		else
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
				hHack ? opacity * 1.5F : opacity);
		
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x3, y1);
		
		GL11.glEnd();
	}
	
	private void drawSettingsBackground(int x2, int x3, int y1, int y2,
		boolean hSettings)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
			hSettings ? opacity * 1.5F : opacity);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
	}
	
	private void drawOutline(int x1, int x2, int y1, int y2)
	{
		float[] acColor = GUI.getAcColor();
		
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
	}
	
	private void drawSeparator(int x3, int y1, int y2)
	{
		// separator
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glEnd();
	}
	
	private void drawSettingsArrow(int x2, int x3, int y1, int y2,
		boolean hSettings)
	{
		double xa1 = x3 + 1;
		double xa2 = (x3 + x2) / 2.0;
		double xa3 = x2 - 1;
		double ya1;
		double ya2;
		
		if(isSettingsWindowOpen())
		{
			ya1 = y2 - 3.5;
			ya2 = y1 + 3;
			GL11.glColor4f(hSettings ? 1 : 0.85F, 0, 0, 1);
			
		}else
		{
			ya1 = y1 + 3.5;
			ya2 = y2 - 3;
			GL11.glColor4f(0, hSettings ? 1 : 0.85F, 0, 1);
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
	}
	
	private void drawName(int x1, int x3, int y1)
	{
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		TextRenderer tr = MC.textRenderer;
		String name = feature.getName();
		int nameWidth = tr.getStringWidth(name);
		int tx = x1 + (x3 - x1 - nameWidth) / 2;
		int ty = y1 + 2;
		
		tr.draw(name, tx, ty, 0xF0F0F0);
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		String name = feature.getName();
		TextRenderer tr = MC.textRenderer;
		int width = tr.getStringWidth(name) + 4;
		if(hasSettings)
			width += 11;
		
		return width;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
