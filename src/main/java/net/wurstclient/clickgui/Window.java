/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import java.util.ArrayList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

public final class Window
{
	private String title;
	private int x;
	private int y;
	private int width;
	private int height;
	
	private boolean valid;
	private final ArrayList<Component> children = new ArrayList<>();
	
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	
	private boolean minimized;
	private boolean minimizable = true;
	
	private boolean pinned;
	private boolean pinnable = true;
	
	private boolean closable;
	private boolean closing;
	
	private boolean invisible;
	
	private int innerHeight;
	private int scrollOffset;
	private boolean scrollingEnabled;
	
	private boolean draggingScrollbar;
	private int scrollbarDragOffsetY;
	
	public Window(String title)
	{
		this.title = title;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public int getX()
	{
		return x;
	}
	
	public void setX(int x)
	{
		this.x = x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public void setY(int y)
	{
		this.y = y;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public void setWidth(int width)
	{
		if(this.width != width)
			invalidate();
		
		this.width = width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public void setHeight(int height)
	{
		if(this.height != height)
			invalidate();
		
		this.height = height;
	}
	
	public void pack()
	{
		int maxChildWidth = 0;
		for(Component c : children)
			if(c.getWidth() > maxChildWidth)
				maxChildWidth = c.getDefaultWidth();
		maxChildWidth += 4;
		
		TextRenderer tr = MinecraftClient.getInstance().textRenderer;
		int titleBarWidth = tr.getStringWidth(title) + 4;
		if(minimizable)
			titleBarWidth += 11;
		if(pinnable)
			titleBarWidth += 11;
		if(closable)
			titleBarWidth += 11;
		
		int childrenHeight = 13;
		for(Component c : children)
			childrenHeight += c.getHeight() + 2;
		childrenHeight += 2;
		
		if(childrenHeight > 200)
		{
			setWidth(Math.max(maxChildWidth + 3, titleBarWidth));
			setHeight(200);
			
		}else
		{
			setWidth(Math.max(maxChildWidth, titleBarWidth));
			setHeight(childrenHeight);
		}
		
		validate();
	}
	
	public void validate()
	{
		if(valid)
			return;
		
		int offsetY = 2;
		int cWidth = width - 4;
		for(Component c : children)
		{
			c.setX(2);
			c.setY(offsetY);
			c.setWidth(cWidth);
			offsetY += c.getHeight() + 2;
		}
		
		innerHeight = offsetY;
		
		scrollingEnabled = innerHeight > height - 13;
		if(scrollingEnabled)
			cWidth -= 3;
		
		for(Component c : children)
			c.setWidth(cWidth);
		
		valid = true;
	}
	
	public void invalidate()
	{
		valid = false;
	}
	
	public int countChildren()
	{
		return children.size();
	}
	
	public Component getChild(int index)
	{
		return children.get(index);
	}
	
	public void add(Component component)
	{
		children.add(component);
		component.setParent(this);
		invalidate();
	}
	
	public void remove(int index)
	{
		children.get(index).setParent(null);
		children.remove(index);
		invalidate();
	}
	
	public void remove(Component component)
	{
		children.remove(component);
		component.setParent(null);
		invalidate();
	}
	
	public boolean isDragging()
	{
		return dragging;
	}
	
	public void startDragging(int mouseX, int mouseY)
	{
		dragging = true;
		dragOffsetX = x - mouseX;
		dragOffsetY = y - mouseY;
	}
	
	public void dragTo(int mouseX, int mouseY)
	{
		x = mouseX + dragOffsetX;
		y = mouseY + dragOffsetY;
	}
	
	public void stopDragging()
	{
		dragging = false;
		dragOffsetX = 0;
		dragOffsetY = 0;
	}
	
	public boolean isMinimized()
	{
		return minimized;
	}
	
	public void setMinimized(boolean minimized)
	{
		this.minimized = minimized;
	}
	
	public boolean isMinimizable()
	{
		return minimizable;
	}
	
	public void setMinimizable(boolean minimizable)
	{
		this.minimizable = minimizable;
	}
	
	public boolean isPinned()
	{
		return pinned;
	}
	
	public void setPinned(boolean pinned)
	{
		this.pinned = pinned;
	}
	
	public boolean isPinnable()
	{
		return pinnable;
	}
	
	public void setPinnable(boolean pinnable)
	{
		this.pinnable = pinnable;
	}
	
	public boolean isClosable()
	{
		return closable;
	}
	
	public void setClosable(boolean closable)
	{
		this.closable = closable;
	}
	
	public boolean isClosing()
	{
		return closing;
	}
	
	public void close()
	{
		closing = true;
	}
	
	public boolean isInvisible()
	{
		return invisible;
	}
	
	public void setInvisible(boolean invisible)
	{
		this.invisible = invisible;
	}
	
	public int getInnerHeight()
	{
		return innerHeight;
	}
	
	public int getScrollOffset()
	{
		return scrollOffset;
	}
	
	public void setScrollOffset(int scrollOffset)
	{
		this.scrollOffset = scrollOffset;
	}
	
	public boolean isScrollingEnabled()
	{
		return scrollingEnabled;
	}
	
	public boolean isDraggingScrollbar()
	{
		return draggingScrollbar;
	}
	
	public void startDraggingScrollbar(int mouseY)
	{
		draggingScrollbar = true;
		double outerHeight = height - 13;
		double scrollbarY =
			outerHeight * (-scrollOffset / (double)innerHeight) + 1;
		scrollbarDragOffsetY = (int)(scrollbarY - mouseY);
	}
	
	public void dragScrollbarTo(int mouseY)
	{
		int scrollbarY = mouseY + scrollbarDragOffsetY;
		double outerHeight = height - 13;
		scrollOffset = (int)((scrollbarY - 1) / outerHeight * innerHeight * -1);
		scrollOffset = Math.min(scrollOffset, 0);
		scrollOffset = Math.max(scrollOffset, -innerHeight + height - 13);
	}
	
	public void stopDraggingScrollbar()
	{
		draggingScrollbar = false;
		scrollbarDragOffsetY = 0;
	}
}
