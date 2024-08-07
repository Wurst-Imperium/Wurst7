/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

public final class ChooseNameScreen extends Screen
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	
	private TextFieldWidget nameField;
	private ButtonWidget doneButton;
	private ButtonWidget cancelButton;
	
	public ChooseNameScreen()
	{
		super(ScreenTexts.EMPTY);
	}
	
	@Override
	public void init()
	{
		TextRenderer tr = client.textRenderer;
		int middleX = width / 2;
		int middleY = height / 2;
		
		nameField = new TextFieldWidget(tr, middleX - 99, middleY + 16, 198, 16,
			Text.empty());
		nameField.setDrawsBackground(false);
		nameField.setMaxLength(32);
		nameField.setFocused(true);
		nameField.setEditableColor(0xFFFFFF);
		addSelectableChild(nameField);
		setFocused(nameField);
		
		doneButton = ButtonWidget.builder(Text.literal("Done"), b -> done())
			.dimensions(middleX - 75, middleY + 38, 150, 20).build();
		addDrawableChild(doneButton);
		
		cancelButton =
			ButtonWidget.builder(Text.literal("Cancel"), b -> cancel())
				.dimensions(middleX - 50, middleY + 62, 100, 15).build();
		addDrawableChild(cancelButton);
	}
	
	private void done()
	{
		client.setScreen(null);
		WURST.getHax().templateToolHack.saveFile();
	}
	
	private void cancel()
	{
		client.setScreen(null);
		WURST.getHax().templateToolHack.setEnabled(false);
	}
	
	@Override
	public void tick()
	{
		if(nameField.getText().isEmpty())
			return;
		
		Path folder = WURST.getHax().autoBuildHack.getFolder();
		Path file = folder.resolve(nameField.getText() + ".json");
		WURST.getHax().templateToolHack.setFile(file.toFile());
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ESCAPE:
			cancelButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ENTER:
			doneButton.onPress();
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		// super.render(context, mouseX, mouseY, partialTicks);
		applyBlur(partialTicks);
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		// middle
		int middleX = width / 2;
		int middleY = height / 2;
		
		// background positions
		int x1 = middleX - 100;
		int y1 = middleY + 15;
		int x2 = middleX + 100;
		int y2 = middleY + 26;
		
		// background
		context.fill(x1, y1, x2, y2, 0x80000000);
		
		// name field
		nameField.render(context, mouseX, mouseY, partialTicks);
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
