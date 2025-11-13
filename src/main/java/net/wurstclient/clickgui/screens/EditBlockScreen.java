/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

public final class EditBlockScreen extends Screen
{
	private final Screen prevScreen;
	private final BlockSetting setting;
	
	private EditBox blockField;
	private Button doneButton;
	
	public EditBlockScreen(Screen prevScreen, BlockSetting setting)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		this.setting = setting;
	}
	
	@Override
	public void init()
	{
		int x1 = width / 2 - 100;
		int y1 = 59;
		int y2 = height / 3 * 2;
		
		Font tr = minecraft.font;
		String valueString = setting.getBlockName();
		
		blockField = new EditBox(tr, x1, y1, 178, 20, Component.literal(""));
		blockField.setValue(valueString);
		blockField.setCursorPosition(0);
		blockField.setMaxLength(256);
		
		addWidget(blockField);
		setFocused(blockField);
		blockField.setFocused(true);
		
		doneButton = Button.builder(Component.literal("Done"), b -> done())
			.bounds(x1, y2, 200, 20).build();
		addRenderableWidget(doneButton);
	}
	
	private void done()
	{
		String nameOrId = blockField.getValue();
		Block block = BlockUtils.getBlockFromNameOrID(nameOrId);
		
		if(block != null)
			setting.setBlock(block);
		
		minecraft.setScreen(prevScreen);
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			done();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			minecraft.setScreen(prevScreen);
			break;
		}
		
		return super.keyPressed(context);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		Matrix3x2fStack matrixStack = context.pose();
		Font tr = minecraft.font;
		
		context.drawCenteredString(tr, setting.getName(), width / 2, 20,
			CommonColors.WHITE);
		
		blockField.render(context, mouseX, mouseY, partialTicks);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		context.guiRenderState.up();
		matrixStack.pushMatrix();
		matrixStack.translate(-64 + width / 2 - 100, 115);
		
		boolean lblAbove =
			!blockField.getValue().isEmpty() || blockField.isFocused();
		String lblText =
			lblAbove ? "Block ID or number:" : "block ID or number";
		int lblX = lblAbove ? 50 : 68;
		int lblY = lblAbove ? -66 : -50;
		int lblColor =
			lblAbove ? WurstColors.VERY_LIGHT_GRAY : CommonColors.GRAY;
		context.drawString(tr, lblText, lblX, lblY, lblColor);
		
		int border = blockField.isFocused() ? CommonColors.WHITE
			: CommonColors.LIGHT_GRAY;
		int black = CommonColors.BLACK;
		
		context.fill(48, -56, 64, -36, border);
		context.fill(49, -55, 65, -37, black);
		context.fill(242, -56, 246, -36, border);
		context.fill(241, -55, 245, -37, black);
		
		matrixStack.popMatrix();
		
		String nameOrId = blockField.getValue();
		Block blockToAdd = BlockUtils.getBlockFromNameOrID(nameOrId);
		
		if(blockToAdd == null)
			blockToAdd = Blocks.AIR;
		
		RenderUtils.drawItem(context, new ItemStack(blockToAdd),
			-64 + width / 2 - 100 + 52, 115 - 52, false);
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
