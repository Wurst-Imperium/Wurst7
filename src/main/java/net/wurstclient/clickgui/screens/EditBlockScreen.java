/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.util.BlockUtils;

public final class EditBlockScreen extends Screen
{
	private final Screen prevScreen;
	private final BlockSetting setting;
	
	private TextFieldWidget blockField;
	private ButtonWidget doneButton;
	
	public EditBlockScreen(Screen prevScreen, BlockSetting setting)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.setting = setting;
	}
	
	@Override
	public void init()
	{
		int x1 = width / 2 - 100;
		int y1 = 60;
		int y2 = height / 3 * 2;
		
		TextRenderer tr = minecraft.textRenderer;
		String valueString = setting.getBlockName();
		
		blockField = new TextFieldWidget(tr, x1, y1, 178, 18, "");
		blockField.setText(valueString);
		blockField.setSelectionStart(0);
		
		children.add(blockField);
		setInitialFocus(blockField);
		blockField.setSelected(true);
		
		doneButton = new ButtonWidget(x1, y2, 200, 20, "Done", b -> done());
		addButton(doneButton);
	}
	
	private void done()
	{
		String value = blockField.getText();
		Block block = BlockUtils.getBlockFromName(value);
		
		if(block != null)
			setting.setBlock(block);
		
		minecraft.openScreen(prevScreen);
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
			minecraft.openScreen(prevScreen);
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		blockField.tick();
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		TextRenderer tr = minecraft.textRenderer;
		
		renderBackground();
		drawCenteredString(tr, setting.getName(), width / 2, 20, 0xFFFFFF);
		
		blockField.render(mouseX, mouseY, partialTicks);
		super.render(mouseX, mouseY, partialTicks);
		
		GL11.glPushMatrix();
		GL11.glTranslated(-64 + width / 2 - 100, 115, 0);
		
		boolean lblAbove =
			!blockField.getText().isEmpty() || blockField.isFocused();
		String lblText = lblAbove ? "Block name or ID:" : "block name or ID";
		int lblX = lblAbove ? 50 : 68;
		int lblY = lblAbove ? -66 : -50;
		int lblColor = lblAbove ? 0xF0F0F0 : 0x808080;
		drawString(tr, lblText, lblX, lblY, lblColor);
		
		fill(48, -56, 64, -36, 0xffa0a0a0);
		fill(49, -55, 64, -37, 0xff000000);
		fill(214, -56, 244, -55, 0xffa0a0a0);
		fill(214, -37, 244, -36, 0xffa0a0a0);
		fill(244, -56, 246, -36, 0xffa0a0a0);
		fill(214, -55, 243, -52, 0xff000000);
		fill(214, -40, 243, -37, 0xff000000);
		fill(215, -55, 216, -37, 0xff000000);
		fill(242, -55, 245, -37, 0xff000000);
		
		Block blockToAdd = BlockUtils.getBlockFromName(blockField.getText());
		renderIcon(new ItemStack(blockToAdd), 52, -52, false);
		
		GL11.glPopMatrix();
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
	
	private void renderIcon(ItemStack stack, int x, int y, boolean large)
	{
		GL11.glPushMatrix();
		
		GL11.glTranslated(x, y, 0);
		double scale = large ? 1.5 : 0.75;
		GL11.glScaled(scale, scale, scale);
		
		DiffuseLighting.enable();
		ItemStack grass = new ItemStack(Blocks.GRASS_BLOCK);
		ItemStack renderStack = !stack.isEmpty() ? stack : grass;
		WurstClient.MC.getItemRenderer().renderGuiItem(renderStack, 0, 0);
		DiffuseLighting.disable();
		
		GL11.glPopMatrix();
		
		if(stack.isEmpty())
			renderQuestionMark(x, y, large);
	}
	
	private void renderQuestionMark(int x, int y, boolean large)
	{
		GL11.glPushMatrix();
		
		GL11.glTranslated(x, y, 0);
		if(large)
			GL11.glScaled(2, 2, 2);
		
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		TextRenderer tr = WurstClient.MC.textRenderer;
		tr.drawWithShadow("?", 3, 2, 0xf0f0f0);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		
		GL11.glPopMatrix();
	}
}
