/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.awt.Rectangle;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.util.RenderUtils;

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
		super(Text.literal(""));
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
	public final boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		// scrollbar
		if(!scrollbarLocked)
		{
			scroll += verticalAmount * 4;
			
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
		
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount,
			verticalAmount);
	}
	
	@Override
	public final void tick()
	{
		onUpdate();
	}
	
	@Override
	public final void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		// background
		int bgx1 = middleX - 154;
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		if(hasBackground)
			drawBackgroundBox(context, bgx1, bgy1, bgx2, bgy2);
		
		// scrollbar
		if(showScrollbar)
		{
			// bar
			int x1 = bgx2 + 16;
			int x2 = x1 + 12;
			int y1 = bgy1;
			int y2 = bgy2;
			drawBackgroundBox(context, x1, y1, x2, y2);
			
			// knob
			x1 += 2;
			x2 -= 2;
			y1 += scrollKnobPosition;
			y2 = y1 + 24;
			drawBackgroundBox(context, x1, y1, x2, y2);
			x1++;
			x2--;
			y1 += 8;
			y2 -= 15;
			for(int i = 0; i < 3; y1 += 4, y2 += 4, i++)
				drawDownShadow(context, x1, y1, x2, y2);
		}
		
		onRender(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY,
		float deltaTicks)
	{
		// Don't blur
	}
	
	@Override
	public final boolean shouldPause()
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
	
	protected abstract void onRender(DrawContext context, int mouseX,
		int mouseY, float partialTicks);
	
	protected final int getStringHeight(String s)
	{
		int fontHeight = client.textRenderer.fontHeight;
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
	
	protected final void drawDownShadow(DrawContext context, int x1, int y1,
		int x2, int y2)
	{
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// line
		int lineColor = RenderUtils.toIntColor(acColor, 0.5F);
		RenderUtils.drawLine2D(context, x1 + 0.1F, y1, x2 + 0.1F, y1,
			lineColor);
		
		// shadow
		int shadowColor1 = RenderUtils.toIntColor(acColor, 0.75F);
		int shadowColor2 = 0x00000000;
		context.fillGradient(x1, y1, x2, y2, shadowColor1, shadowColor2);
	}
	
	protected final void drawBox(DrawContext context, int x1, int y1, int x2,
		int y2, int color)
	{
		context.fill(x1, y1, x2, y2, color);
		RenderUtils.drawBoxShadow2D(context, x1, y1, x2, y2);
	}
	
	protected final int getBackgroundColor()
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		gui.updateColors();
		return RenderUtils.toIntColor(gui.getBgColor(), gui.getOpacity());
	}
	
	protected final void drawBackgroundBox(DrawContext context, int x1, int y1,
		int x2, int y2)
	{
		drawBox(context, x1, y1, x2, y2, getBackgroundColor());
	}
}
