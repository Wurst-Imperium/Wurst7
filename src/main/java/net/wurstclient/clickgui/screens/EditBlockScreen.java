/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.lwjgl.glfw.GLFW;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;

public final class EditBlockScreen extends Screen
{
	private final Screen prevScreen;
	private final BlockSetting setting;
	
	private TextFieldWidget blockField;
	private ButtonWidget doneButton;
	
	public EditBlockScreen(Screen prevScreen, BlockSetting setting)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.setting = setting;
	}
	
	@Override
	public void init()
	{
		int x1 = width / 2 - 100;
		int y1 = 59;
		int y2 = height / 3 * 2;
		
		TextRenderer tr = client.textRenderer;
		String valueString = setting.getBlockName();
		
		blockField = new TextFieldWidget(tr, x1, y1, 178, 20, Text.literal(""));
		blockField.setText(valueString);
		blockField.setSelectionStart(0);
		blockField.setMaxLength(256);
		
		addSelectableChild(blockField);
		setFocused(blockField);
		blockField.setFocused(true);
		
		doneButton = ButtonWidget.builder(Text.literal("Done"), b -> done())
			.dimensions(x1, y2, 200, 20).build();
		addDrawableChild(doneButton);
	}
	
	private void done()
	{
		String nameOrId = blockField.getText();
		Block block = BlockUtils.getBlockFromNameOrID(nameOrId);
		
		if(block != null)
			setting.setBlock(block);
		
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
		MatrixStack matrixStack = context.getMatrices();
		TextRenderer tr = client.textRenderer;
		
		renderBackground(context, mouseX, mouseY, partialTicks);
		context.drawCenteredTextWithShadow(tr, setting.getName(), width / 2, 20,
			0xFFFFFF);
		
		blockField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.push();
		matrixStack.translate(-64 + width / 2 - 100, 115, 0);
		
		boolean lblAbove =
			!blockField.getText().isEmpty() || blockField.isFocused();
		String lblText =
			lblAbove ? "Block ID or number:" : "block ID or number";
		int lblX = lblAbove ? 50 : 68;
		int lblY = lblAbove ? -66 : -50;
		int lblColor = lblAbove ? 0xF0F0F0 : 0x808080;
		context.drawTextWithShadow(tr, lblText, lblX, lblY, lblColor);
		
		int border = blockField.isFocused() ? 0xffffffff : 0xffa0a0a0;
		int black = 0xff000000;
		
		context.fill(48, -56, 64, -36, border);
		context.fill(49, -55, 65, -37, black);
		context.fill(242, -56, 246, -36, border);
		context.fill(241, -55, 245, -37, black);
		
		matrixStack.pop();
		
		String nameOrId = blockField.getText();
		Block blockToAdd = BlockUtils.getBlockFromNameOrID(nameOrId);
		
		if(blockToAdd == null)
			blockToAdd = Blocks.AIR;
		
		RenderUtils.drawItem(context, new ItemStack(blockToAdd),
			-64 + width / 2 - 100 + 52, 115 - 52, false);
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
