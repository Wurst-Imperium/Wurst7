/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
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
		String valueString = setting.getBlockName();
		
		blockField = new TextFieldWidget(tr, x1, y1, 178, 18, Text.literal(""));
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
	public void tick()
	{
		blockField.tick();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		TextRenderer tr = client.textRenderer;
		
		renderBackground(matrixStack);
		drawCenteredTextWithShadow(matrixStack, tr, setting.getName(),
			width / 2, 20, 0xFFFFFF);
		
		blockField.render(matrixStack, mouseX, mouseY, partialTicks);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		
		matrixStack.push();
		matrixStack.translate(-64 + width / 2 - 100, 115, 0);
		
		boolean lblAbove =
			!blockField.getText().isEmpty() || blockField.isFocused();
		String lblText =
			lblAbove ? "Block ID or number:" : "block ID or number";
		int lblX = lblAbove ? 50 : 68;
		int lblY = lblAbove ? -66 : -50;
		int lblColor = lblAbove ? 0xF0F0F0 : 0x808080;
		drawTextWithShadow(matrixStack, tr, lblText, lblX, lblY, lblColor);
		
		int border = blockField.isFocused() ? 0xffffffff : 0xffa0a0a0;
		int black = 0xff000000;
		
		fill(matrixStack, 48, -56, 64, -36, border);
		fill(matrixStack, 49, -55, 64, -37, black);
		fill(matrixStack, 214, -56, 244, -55, border);
		fill(matrixStack, 214, -37, 244, -36, border);
		fill(matrixStack, 244, -56, 246, -36, border);
		fill(matrixStack, 214, -55, 243, -52, black);
		fill(matrixStack, 214, -40, 243, -37, black);
		fill(matrixStack, 215, -55, 216, -37, black);
		fill(matrixStack, 242, -55, 245, -37, black);
		
		matrixStack.pop();
		
		String nameOrId = blockField.getText();
		Block blockToAdd = BlockUtils.getBlockFromNameOrID(nameOrId);
		
		if(blockToAdd == null)
			blockToAdd = Blocks.AIR;
		
		renderIcon(matrixStack, new ItemStack(blockToAdd),
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
	
	private void renderIcon(MatrixStack matrixStack, ItemStack stack, int x,
		int y, boolean large)
	{
		MatrixStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.push();
		
		modelViewStack.translate(x, y, 0);
		float scale = large ? 1.5F : 0.75F;
		modelViewStack.scale(scale, scale, scale);
		
		DiffuseLighting.enableGuiDepthLighting();
		ItemStack grass = new ItemStack(Blocks.GRASS_BLOCK);
		ItemStack renderStack = !stack.isEmpty() ? stack : grass;
		WurstClient.MC.getItemRenderer().renderInGuiWithOverrides(renderStack,
			0, 0);
		DiffuseLighting.disableGuiDepthLighting();
		
		modelViewStack.pop();
		RenderSystem.applyModelViewMatrix();
		
		if(stack.isEmpty())
			renderQuestionMark(matrixStack, x, y, large);
	}
	
	private void renderQuestionMark(MatrixStack matrixStack, int x, int y,
		boolean large)
	{
		matrixStack.push();
		
		matrixStack.translate(x, y, 0);
		if(large)
			matrixStack.scale(2, 2, 2);
		
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		TextRenderer tr = WurstClient.MC.textRenderer;
		tr.drawWithShadow(matrixStack, "?", 3, 2, 0xf0f0f0);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		
		matrixStack.pop();
	}
}
