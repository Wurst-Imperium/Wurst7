/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.MathUtils;

public final class EditSliderScreen extends Screen
{
	private final Screen prevScreen;
	private final SliderSetting slider;
	
	private TextFieldWidget valueField;
	private ButtonWidget doneButton;
	
	public EditSliderScreen(Screen prevScreen, SliderSetting slider)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.slider = slider;
	}
	
	@Override
	public void init()
	{
		int x1 = width / 2 - 100;
		int y1 = 60;
		int y2 = height / 3 * 2;
		
		TextRenderer tr = client.textRenderer;
		ValueDisplay vd = ValueDisplay.DECIMAL;
		String valueString = vd.getValueString(slider.getValue());
		
		valueField =
			new TextFieldWidget(tr, x1, y1, 200, 20, new LiteralText(""));
		valueField.setText(valueString);
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
		
		if(MathUtils.isDouble(value))
			slider.setValue(Double.parseDouble(value));
		
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
		drawCenteredString(matrixStack, client.textRenderer, slider.getName(),
			width / 2, 20, 0xFFFFFF);
		
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
