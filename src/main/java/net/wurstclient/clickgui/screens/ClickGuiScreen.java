/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import net.minecraft.client.gui.DrawContext;
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
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		gui.handleMouseClick((int)mouseX, (int)mouseY, mouseButton);
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int mouseButton)
	{
		gui.handleMouseRelease(mouseX, mouseY, mouseButton);
		return super.mouseReleased(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta)
	{
		gui.handleMouseScroll(mouseX, mouseY, delta);
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		super.render(context, mouseX, mouseY, partialTicks);
		gui.render(context, mouseX, mouseY, partialTicks);
	}
}
