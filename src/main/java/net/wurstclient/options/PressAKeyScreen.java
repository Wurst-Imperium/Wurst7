/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class PressAKeyScreen extends Screen
{
	private PressAKeyCallback prevScreen;
	
	public PressAKeyScreen(PressAKeyCallback prevScreen)
	{
		super(Component.literal(""));
		
		if(!(prevScreen instanceof Screen))
			throw new IllegalArgumentException("prevScreen is not a screen");
		
		this.prevScreen = prevScreen;
	}
	
	@Override
	public boolean keyPressed(KeyEvent event)
	{
		if(event.key() != GLFW.GLFW_KEY_ESCAPE)
			prevScreen.setKey(InputConstants.getKey(event).getName());
		
		minecraft.setScreen((Screen)prevScreen);
		return super.keyPressed(event);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
	{
		prevScreen.setKey(
			InputConstants.Type.MOUSE.getOrCreate(event.button()).getName());
		minecraft.setScreen((Screen)prevScreen);
		return true;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		context.drawCenteredString(font, "Press a key or mouse button",
			width / 2, height / 4 + 48, CommonColors.WHITE);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
}
