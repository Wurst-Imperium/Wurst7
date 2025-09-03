/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import net.minecraft.class_11909;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.wurstclient.clickgui.ClickGui;

public final class ClickGuiScreen extends Screen
{
	private final ClickGui gui;
	
	public ClickGuiScreen(ClickGui gui)
	{
		super(Text.literal(""));
		this.gui = gui;
	}
	
	@Override
	public boolean shouldPause()
	{
		return false;
	}
	
	@Override
	public boolean mouseClicked(class_11909 context, boolean doubleClick)
	{
		gui.handleMouseClick(context);
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public boolean mouseReleased(class_11909 context)
	{
		gui.handleMouseRelease(context.x(), context.y(),
			context.method_74245());
		return super.mouseReleased(context);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		gui.handleMouseScroll(mouseX, mouseY, verticalAmount);
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount,
			verticalAmount);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		gui.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY,
		float deltaTicks)
	{
		// Don't blur
	}
}
