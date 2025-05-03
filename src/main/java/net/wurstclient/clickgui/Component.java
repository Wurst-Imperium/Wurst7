/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.wurstclient.WurstClient;

public abstract class Component
{
	protected static final MinecraftClient MC = WurstClient.MC;
	protected static final WurstClient WURST = WurstClient.INSTANCE;
	
	private int x;
	private int y;
	private int width;
	private int height;
	
	private Window parent;
	
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		
	}
	
	public abstract void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks);
	
	public abstract int getDefaultWidth();
	
	public abstract int getDefaultHeight();
	
	public int getX()
	{
		return x;
	}
	
	public void setX(int x)
	{
		if(this.x != x)
			invalidateParent();
		
		this.x = x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public void setY(int y)
	{
		if(this.y != y)
			invalidateParent();
		
		this.y = y;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public void setWidth(int width)
	{
		if(this.width != width)
			invalidateParent();
		
		this.width = width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public void setHeight(int height)
	{
		if(this.height != height)
			invalidateParent();
		
		this.height = height;
	}
	
	public Window getParent()
	{
		return parent;
	}
	
	public void setParent(Window parent)
	{
		this.parent = parent;
	}
	
	private void invalidateParent()
	{
		if(parent != null)
			parent.invalidate();
	}
	
	protected boolean isHovering(int mouseX, int mouseY)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		Window parent = getParent();
		boolean scrollEnabled = parent.isScrollingEnabled();
		int scroll = scrollEnabled ? parent.getScrollOffset() : 0;
		
		return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2
			&& mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
	}
}
