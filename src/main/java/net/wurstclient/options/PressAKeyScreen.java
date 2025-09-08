/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class PressAKeyScreen extends Screen
{
	private PressAKeyCallback prevScreen;
	
	public PressAKeyScreen(PressAKeyCallback prevScreen)
	{
		super(Text.literal(""));
		
		if(!(prevScreen instanceof Screen))
			throw new IllegalArgumentException("prevScreen is not a screen");
		
		this.prevScreen = prevScreen;
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
	{
		if(context.key() != GLFW.GLFW_KEY_ESCAPE)
			prevScreen.setKey(getKeyName(context));
		
		client.setScreen((Screen)prevScreen);
		return super.keyPressed(context);
	}
	
	private String getKeyName(KeyInput context)
	{
		return InputUtil.fromKeyCode(context).getTranslationKey();
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		context.drawCenteredTextWithShadow(textRenderer, "Press a key",
			width / 2, height / 4 + 48, Colors.WHITE);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
}
