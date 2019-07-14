/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;

public final class FeatureButton extends Component
{
	private final Feature feature;
	private Window settingsWindow;
	
	public FeatureButton(Feature feature)
	{
		this.feature = feature;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(int mouseX, int mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
			
		// if(!feature.getSettings().isEmpty()
		// && mouseX > getX() + getWidth() - 12)
		// {
		// if(settingsWindow != null && !settingsWindow.isClosing())
		// {
		// settingsWindow.close();
		// settingsWindow = null;
		// return;
		// }
		//
		// settingsWindow = new Window(feature.getName() + " Settings");
		// for(Setting setting : feature.getSettings())
		// settingsWindow.add(setting.getComponent());
		//
		// settingsWindow.setClosable(true);
		// settingsWindow.setMinimizable(false);
		// settingsWindow.pack();
		//
		// int scroll = getParent().isScrollingEnabled()
		// ? getParent().getScrollOffset() : 0;
		// int x = getParent().getX() + getParent().getWidth() + 5;
		// int y = getParent().getY() + 12 + getY() + scroll;
		// ScaledResolution sr =
		// new ScaledResolution(Minecraft.getMinecraft());
		// if(x + settingsWindow.getWidth() > sr.getScaledWidth())
		// x = getParent().getX() - settingsWindow.getWidth() - 5;
		// if(y + settingsWindow.getHeight() > sr.getScaledHeight())
		// y -= settingsWindow.getHeight() - 14;
		// settingsWindow.setX(x);
		// settingsWindow.setY(y);
		//
		// ClickGui gui = WurstClient.INSTANCE.getGui();
		// gui.addWindow(settingsWindow);
		// return;
		// }
		
		feature.doPrimaryAction();
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		// boolean settings = !feature.getSettings().isEmpty();
		boolean settings = !"".isEmpty();
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = settings ? x2 - 11 : x2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		int scroll = getParent().isScrollingEnabled()
			? getParent().getScrollOffset() : 0;
		boolean hovering = mouseX >= x1 && mouseY >= y1 && mouseX < x2
			&& mouseY < y2 && mouseY >= -scroll
			&& mouseY < getParent().getHeight() - 13 - scroll;
		boolean hHack = hovering && mouseX < x3;
		boolean hSettings = hovering && mouseX >= x3;
		
		// tooltip
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
		
		if(hHack)
			gui.setTooltip(tooltip);
		
		// color
		if(feature.isEnabled())
			// if(feature.isBlocked())
			// glColor4f(1, 0, 0, hHack ? opacity * 1.5F : opacity);
			// else
			GL11.glColor4f(0, 1, 0, hHack ? opacity * 1.5F : opacity);
		else
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
				hHack ? opacity * 1.5F : opacity);
		
		// background
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x3, y1);
		if(settings)
		{
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
				hSettings ? opacity * 1.5F : opacity);
			GL11.glVertex2i(x3, y1);
			GL11.glVertex2i(x3, y2);
			GL11.glVertex2i(x2, y2);
			GL11.glVertex2i(x2, y1);
		}
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		if(settings)
		{
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
			
			if(settingsWindow != null && !settingsWindow.isClosing())
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
		
		// hack name
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TextRenderer fr = WurstClient.MC.textRenderer;
		int fx = x1 + ((settings ? getWidth() - 11 : getWidth())
			- fr.getStringWidth(feature.getName())) / 2;
		int fy = y1 + 2;
		fr.draw(feature.getName(), fx, fy, 0xf0f0f0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer fr = WurstClient.MC.textRenderer;
		int width = fr.getStringWidth(feature.getName()) + 2;
		// if(!feature.getSettings().isEmpty())
		// width += 11;
		return width;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
