/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public final class EnterProfileNameScreen extends Screen
{
	private final Screen prevScreen;
	private final Consumer<String> callback;
	
	private TextFieldWidget valueField;
	private ButtonWidget doneButton;
	
	public EnterProfileNameScreen(Screen prevScreen, Consumer<String> callback)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.callback = callback;
	}
	
	@Override
	public void init()
	{
		int x1 = width / 2 - 100;
		int y1 = 60;
		int y2 = height / 3 * 2;
		
		TextRenderer tr = client.textRenderer;
		
		valueField =
			new TextFieldWidget(tr, x1, y1, 200, 20, new LiteralText(""));
		valueField.setText("");
		valueField.setSelectionStart(0);
		
		children.add(valueField);
		setInitialFocus(valueField);
		valueField.setSelected(true);
		
		doneButton = new ButtonWidget(x1, y2, 200, 20, new LiteralText("Done"),
			b -> done());
		addButton(doneButton);
	}
	
	private void done()
	{
		String value = valueField.getText();
		if(!value.isEmpty())
			callback.accept(value);
		
		client.openScreen(prevScreen);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			done();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			client.openScreen(prevScreen);
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		valueField.tick();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		drawCenteredString(matrixStack, client.textRenderer,
			"Name your new profile", width / 2, 20, 0xFFFFFF);
		
		valueField.render(matrixStack, mouseX, mouseY, partialTicks);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
}
