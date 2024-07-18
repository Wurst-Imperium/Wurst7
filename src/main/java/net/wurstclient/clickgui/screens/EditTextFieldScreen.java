/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.wurstclient.settings.TextFieldSetting;

public final class EditTextFieldScreen extends Screen
{
	private final Screen prevScreen;
	private final TextFieldSetting setting;
	
	private TextFieldWidget valueField;
	private ButtonWidget doneButton;
	
	public EditTextFieldScreen(Screen prevScreen, TextFieldSetting setting)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.setting = setting;
	}
	
	@Override
	public void init()
	{
		int x1 = width / 2 - 100;
		int y1 = 60;
		int y2 = height / 3 * 2;
		
		TextRenderer tr = client.textRenderer;
		
		valueField = new TextFieldWidget(tr, x1, y1, 200, 20, Text.literal(""));
		valueField.setMaxLength(Integer.MAX_VALUE);
		valueField.setText(setting.getValue());
		valueField.setSelectionStart(0);
		
		addSelectableChild(valueField);
		setFocused(valueField);
		valueField.setFocused(true);
		
		doneButton = ButtonWidget.builder(Text.literal("Done"), b -> done())
			.dimensions(x1, y2, 200, 20).build();
		addDrawableChild(doneButton);
	}
	
	private void done()
	{
		String value = valueField.getText();
		setting.setValue(value);
		
		client.setScreen(prevScreen);
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
			client.setScreen(prevScreen);
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context, mouseX, mouseY, partialTicks);
		context.drawCenteredTextWithShadow(client.textRenderer,
			setting.getName(), width / 2, 20, 0xFFFFFF);
		
		valueField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean shouldPause()
	{
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
}
