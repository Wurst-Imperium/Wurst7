/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class PressAKeyScreen extends Screen
{
	private PressAKeyCallback prevScreen;
	
	public PressAKeyScreen(PressAKeyCallback prevScreen)
	{
		super(new LiteralText(""));
		
		if(!(prevScreen instanceof Screen))
			throw new IllegalArgumentException("prevScreen is not a screen");
		
		this.prevScreen = prevScreen;
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode != GLFW.GLFW_KEY_ESCAPE)
			prevScreen.setKey(getKeyName(keyCode, scanCode));
		
		client.openScreen((Screen)prevScreen);
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	private String getKeyName(int keyCode, int scanCode)
	{
		return InputUtil.fromKeyCode(keyCode, scanCode).getTranslationKey();
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		drawCenteredString(matrixStack, textRenderer, "Press a key", width / 2,
			height / 4 + 48, 16777215);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
}
