/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Rectangle;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;

public abstract class NavigatorScreen extends Screen
{
	protected int scroll = 0;
	private int scrollKnobPosition = 2;
	private boolean scrolling;
	private int maxScroll;
	protected boolean scrollbarLocked;
	protected int middleX;
	protected boolean hasBackground = true;
	protected int nonScrollableArea = 26;
	private boolean showScrollbar;
	
	public NavigatorScreen()
	{
		super(new LiteralText(""));
	}
	
	@Override
	protected final void init()
	{
		middleX = width / 2;
		onResize();
	}
	
	@Override
	public final boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		onKeyPress(keyCode, scanCode, int_3);
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public final boolean mouseClicked(double x, double y, int button)
	{
		// scrollbar
		if(new Rectangle(width / 2 + 170, 60, 12, height - 103).contains(x, y))
			scrolling = true;
		
		onMouseClick(x, y, button);
		
		// vanilla buttons
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public final boolean mouseDragged(double mouseX, double mouseY,
		int mouseButton, double double_3, double double_4)
	{
		// scrollbar
		if(scrolling && !scrollbarLocked && mouseButton == 0)
		{
			if(maxScroll == 0)
				scroll = 0;
			else
				scroll = (int)((mouseY - 72) * maxScroll / (height - 131));
			
			if(scroll > 0)
				scroll = 0;
			else if(scroll < maxScroll)
				scroll = maxScroll;
			
			if(maxScroll == 0)
				scrollKnobPosition = 0;
			else
				scrollKnobPosition =
					(int)((height - 131) * scroll / (float)maxScroll);
			scrollKnobPosition += 2;
		}
		
		onMouseDrag(mouseX, mouseY, mouseButton, double_3, double_4);
		
		return super.mouseDragged(mouseX, mouseY, mouseButton, double_3,
			double_4);
	}
	
	@Override
	public final boolean mouseReleased(double x, double y, int button)
	{
		// scrollbar
		scrolling = false;
		
		onMouseRelease(x, y, button);
		
		// vanilla buttons
		return super.mouseReleased(x, y, button);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta)
	{
		// scrollbar
		if(!scrollbarLocked)
		{
			scroll += delta * 4;
			
			if(scroll > 0)
				scroll = 0;
			else if(scroll < maxScroll)
				scroll = maxScroll;
			
			if(maxScroll == 0)
				scrollKnobPosition = 0;
			else
				scrollKnobPosition =
					(int)((height - 131) * scroll / (float)maxScroll);
			scrollKnobPosition += 2;
		}
		
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override
	public final void tick()
	{
		onUpdate();
	}
	
	@Override
	public final void render(int mouseX, int mouseY, float partialTicks)
	{
		// GL settings
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_CULL_FACE);
		glDisable(GL_TEXTURE_2D);
		glShadeModel(GL_SMOOTH);
		
		// background
		int bgx1 = middleX - 154;
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		if(hasBackground)
			drawBackgroundBox(bgx1, bgy1, bgx2, bgy2);
		
		// scrollbar
		if(showScrollbar)
		{
			// bar
			int x1 = bgx2 + 16;
			int x2 = x1 + 12;
			int y1 = bgy1;
			int y2 = bgy2;
			drawBackgroundBox(x1, y1, x2, y2);
			
			// knob
			x1 += 2;
			x2 -= 2;
			y1 += scrollKnobPosition;
			y2 = y1 + 24;
			drawForegroundBox(x1, y1, x2, y2);
			int i;
			for(x1++, x2--, y1 += 8, y2 -= 15, i = 0; i < 3; y1 += 4, y2 +=
				4, i++)
				drawDownShadow(x1, y1, x2, y2);
		}
		
		onRender(mouseX, mouseY, partialTicks);
		
		// GL resets
		glEnable(GL_CULL_FACE);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
	}
	
	@Override
	public final boolean isPauseScreen()
	{
		return false;
	}
	
	protected abstract void onResize();
	
	protected abstract void onKeyPress(int keyCode, int scanCode, int int_3);
	
	protected abstract void onMouseClick(double x, double y, int button);
	
	protected abstract void onMouseDrag(double mouseX, double mouseY,
		int button, double double_3, double double_4);
	
	protected abstract void onMouseRelease(double x, double y, int button);
	
	protected abstract void onUpdate();
	
	protected abstract void onRender(int mouseX, int mouseY,
		float partialTicks);
	
	protected final int getStringHeight(String s)
	{
		int fontHeight = minecraft.textRenderer.fontHeight;
		int height = fontHeight;
		
		for(int i = 0; i < s.length(); i++)
			if(s.charAt(i) == '\n')
				height += fontHeight;
			
		return height;
	}
	
	protected final void setContentHeight(int contentHeight)
	{
		maxScroll = height - contentHeight - nonScrollableArea - 120;
		if(maxScroll > 0)
			maxScroll = 0;
		showScrollbar = maxScroll != 0;
		
		if(scroll < maxScroll)
			scroll = maxScroll;
	}
	
	protected final void drawQuads(int x1, int y1, int x2, int y2)
	{
		glBegin(GL_QUADS);
		{
			glVertex2i(x1, y1);
			glVertex2i(x2, y1);
			glVertex2i(x2, y2);
			glVertex2i(x1, y2);
		}
		glEnd();
	}
	
	protected final void drawBoxShadow(int x1, int y1, int x2, int y2)
	{
		// color
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// outline positions
		double xi1 = x1 - 0.1;
		double xi2 = x2 + 0.1;
		double yi1 = y1 - 0.1;
		double yi2 = y2 + 0.1;
		
		// outline
		glLineWidth(1F);
		glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		glBegin(GL_LINE_LOOP);
		{
			glVertex2d(xi1, yi1);
			glVertex2d(xi2, yi1);
			glVertex2d(xi2, yi2);
			glVertex2d(xi1, yi2);
		}
		glEnd();
		
		// shadow positions
		xi1 -= 0.9;
		xi2 += 0.9;
		yi1 -= 0.9;
		yi2 += 0.9;
		
		// top left
		glBegin(GL_POLYGON);
		{
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2d(x1, y1);
			glVertex2d(x2, y1);
			glColor4f(0F, 0F, 0F, 0F);
			glVertex2d(xi2, yi1);
			glVertex2d(xi1, yi1);
			glVertex2d(xi1, yi2);
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2d(x1, y2);
		}
		glEnd();
		
		// bottom right
		glBegin(GL_POLYGON);
		{
			glVertex2d(x2, y2);
			glVertex2d(x2, y1);
			glColor4f(0F, 0F, 0F, 0F);
			glVertex2d(xi2, yi1);
			glVertex2d(xi2, yi2);
			glVertex2d(xi1, yi2);
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2d(x1, y2);
		}
		glEnd();
	}
	
	protected final void drawInvertedBoxShadow(int x1, int y1, int x2, int y2)
	{
		// color
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// outline positions
		double xi1 = x1 + 0.1;
		double xi2 = x2 - 0.1;
		double yi1 = y1 + 0.1;
		double yi2 = y2 - 0.1;
		
		// outline
		glLineWidth(1F);
		glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		glBegin(GL_LINE_LOOP);
		{
			glVertex2d(xi1, yi1);
			glVertex2d(xi2, yi1);
			glVertex2d(xi2, yi2);
			glVertex2d(xi1, yi2);
		}
		glEnd();
		
		// shadow positions
		xi1 += 0.9;
		xi2 -= 0.9;
		yi1 += 0.9;
		yi2 -= 0.9;
		
		// top left
		glBegin(GL_POLYGON);
		{
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2d(x1, y1);
			glVertex2d(x2, y1);
			glColor4f(0F, 0F, 0F, 0F);
			glVertex2d(xi2, yi1);
			glVertex2d(xi1, yi1);
			glVertex2d(xi1, yi2);
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2d(x1, y2);
		}
		glEnd();
		
		// bottom right
		glBegin(GL_POLYGON);
		{
			glVertex2d(x2, y2);
			glVertex2d(x2, y1);
			glColor4f(0F, 0F, 0F, 0F);
			glVertex2d(xi2, yi1);
			glVertex2d(xi2, yi2);
			glVertex2d(xi1, yi2);
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2d(x1, y2);
		}
		glEnd();
	}
	
	protected final void drawDownShadow(int x1, int y1, int x2, int y2)
	{
		// color
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// outline
		double yi1 = y1 + 0.1;
		glLineWidth(1F);
		glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		glBegin(GL_LINES);
		{
			glVertex2d(x1, yi1);
			glVertex2d(x2, yi1);
		}
		glEnd();
		
		// shadow
		glBegin(GL_POLYGON);
		{
			glColor4f(acColor[0], acColor[1], acColor[2], 0.75F);
			glVertex2i(x1, y1);
			glVertex2i(x2, y1);
			glColor4f(0F, 0F, 0F, 0F);
			glVertex2i(x2, y2);
			glVertex2i(x1, y2);
		}
		glEnd();
	}
	
	protected final void drawBox(int x1, int y1, int x2, int y2)
	{
		drawQuads(x1, y1, x2, y2);
		drawBoxShadow(x1, y1, x2, y2);
	}
	
	protected final void drawEngravedBox(int x1, int y1, int x2, int y2)
	{
		drawQuads(x1, y1, x2, y2);
		drawInvertedBoxShadow(x1, y1, x2, y2);
	}
	
	protected final void setColorToBackground()
	{
		WurstClient.INSTANCE.getGui().updateColors();
		float[] bgColor = WurstClient.INSTANCE.getGui().getBgColor();
		float opacity = WurstClient.INSTANCE.getGui().getOpacity();
		glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
	}
	
	protected final void setColorToForeground()
	{
		WurstClient.INSTANCE.getGui().updateColors();
		float[] bgColor = WurstClient.INSTANCE.getGui().getBgColor();
		float opacity = WurstClient.INSTANCE.getGui().getOpacity();
		glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
	}
	
	protected final void drawBackgroundBox(int x1, int y1, int x2, int y2)
	{
		setColorToBackground();
		drawBox(x1, y1, x2, y2);
	}
	
	protected final void drawForegroundBox(int x1, int y1, int x2, int y2)
	{
		setColorToForeground();
		drawBox(x1, y1, x2, y2);
	}
}
