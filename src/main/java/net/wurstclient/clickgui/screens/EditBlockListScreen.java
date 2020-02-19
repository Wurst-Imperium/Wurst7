/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.util.BlockUtils;

public final class EditBlockListScreen extends Screen
{
	private final Screen prevScreen;
	private final BlockListSetting blockList;
	
	private ListGui listGui;
	private TextFieldWidget blockNameField;
	private ButtonWidget addButton;
	private ButtonWidget removeButton;
	private ButtonWidget doneButton;
	
	private Block blockToAdd;
	
	public EditBlockListScreen(Screen prevScreen, BlockListSetting blockList)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.blockList = blockList;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, blockList.getBlockNames());
		
		blockNameField = new TextFieldWidget(minecraft.textRenderer,
			width / 2 - 152, height - 55, 150, 18, "");
		children.add(blockNameField);
		
		addButton(addButton =
			new ButtonWidget(width / 2 - 2, height - 56, 30, 20, "Add", b -> {
				blockList.add(blockToAdd);
				blockNameField.setText("");
			}));
		
		addButton(removeButton =
			new ButtonWidget(width / 2 + 52, height - 56, 100, 20,
				"Remove Selected", b -> blockList.remove(listGui.selected)));
		
		addButton(new ButtonWidget(width - 108, 8, 100, 20, "Reset to Defaults",
			b -> minecraft.openScreen(new ConfirmScreen(b2 -> {
				if(b2)
					blockList.resetToDefaults();
				minecraft.openScreen(EditBlockListScreen.this);
			}, new LiteralText("Reset to Defaults"),
				new LiteralText("Are you sure?")))));
		
		addButton(doneButton = new ButtonWidget(width / 2 - 100, height - 28,
			200, 20, "Done", b -> minecraft.openScreen(prevScreen)));
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		blockNameField.mouseClicked(mouseX, mouseY, mouseButton);
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked && (mouseX < (width - 220) / 2
			|| mouseX > width / 2 + 129 || mouseY < 32 || mouseY > height - 64))
			listGui.selected = -1;
		
		return childClicked;
	}
	
	@Override
	public boolean mouseDragged(double double_1, double double_2, int int_1,
		double double_3, double double_4)
	{
		listGui.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		return super.mouseDragged(double_1, double_2, int_1, double_3,
			double_4);
	}
	
	@Override
	public boolean mouseReleased(double double_1, double double_2, int int_1)
	{
		listGui.mouseReleased(double_1, double_2, int_1);
		return super.mouseReleased(double_1, double_2, int_1);
	}
	
	@Override
	public boolean mouseScrolled(double double_1, double double_2,
		double double_3)
	{
		listGui.mouseScrolled(double_1, double_2, double_3);
		return super.mouseScrolled(double_1, double_2, double_3);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			addButton.onPress();
		else if(keyCode == GLFW.GLFW_KEY_DELETE)
			removeButton.onPress();
		else if(keyCode == GLFW.GLFW_KEY_ESCAPE)
			doneButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		blockNameField.tick();
		
		blockToAdd = BlockUtils.getBlockFromName(blockNameField.getText());
		addButton.active = blockToAdd != null;
		
		removeButton.active =
			listGui.selected >= 0 && listGui.selected < listGui.list.size();
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		renderBackground();
		listGui.render(mouseX, mouseY, partialTicks);
		
		drawCenteredString(minecraft.textRenderer,
			blockList.getName() + " (" + listGui.getItemCount() + ")",
			width / 2, 12, 0xffffff);
		
		blockNameField.render(mouseX, mouseY, partialTicks);
		super.render(mouseX, mouseY, partialTicks);
		
		GL11.glPushMatrix();
		GL11.glTranslated(-64 + width / 2 - 152, 0, 0);
		GL11.glTranslated(0, 0, 300);
		
		if(blockNameField.getText().isEmpty() && !blockNameField.isFocused())
			drawString(minecraft.textRenderer, "block name or ID", 68,
				height - 50, 0x808080);
		
		fill(48, height - 56, 64, height - 36, 0xffa0a0a0);
		fill(49, height - 55, 64, height - 37, 0xff000000);
		fill(214, height - 56, 244, height - 55, 0xffa0a0a0);
		fill(214, height - 37, 244, height - 36, 0xffa0a0a0);
		fill(244, height - 56, 246, height - 36, 0xffa0a0a0);
		fill(214, height - 55, 243, height - 52, 0xff000000);
		fill(214, height - 40, 243, height - 37, 0xff000000);
		fill(214, height - 55, 216, height - 37, 0xff000000);
		fill(242, height - 55, 245, height - 37, 0xff000000);
		listGui.renderIconAndGetName(new ItemStack(blockToAdd), 52, height - 52,
			false);
		
		GL11.glPopMatrix();
	}
	
	private static class ListGui extends ListWidget
	{
		private final MinecraftClient mc;
		private final List<String> list;
		private int selected = -1;
		
		public ListGui(MinecraftClient mc, EditBlockListScreen screen,
			List<String> list)
		{
			super(mc, screen.width, screen.height, 32, screen.height - 64, 30);
			this.mc = mc;
			this.list = list;
		}
		
		@Override
		protected int getItemCount()
		{
			return list.size();
		}
		
		@Override
		protected boolean selectItem(int index, int int_2, double var3,
			double var4)
		{
			if(index >= 0 && index < list.size())
				selected = index;
			
			return true;
		}
		
		@Override
		protected boolean isSelectedItem(int index)
		{
			return index == selected;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(int index, int x, int y, int var4, int var5,
			int var6, float partialTicks)
		{
			String name = list.get(index);
			ItemStack stack = new ItemStack(BlockUtils.getBlockFromName(name));
			TextRenderer fr = mc.textRenderer;
			
			String displayName =
				renderIconAndGetName(stack, x + 1, y + 1, true);
			fr.draw(displayName, x + 28, y, 0xf0f0f0);
			fr.draw(name, x + 28, y + 9, 0xa0a0a0);
			fr.draw("ID: " + BlockUtils.getBlockFromName(name), x + 28, y + 18,
				0xa0a0a0);
		}
		
		private String renderIconAndGetName(ItemStack stack, int x, int y,
			boolean large)
		{
			if(stack.isEmpty())
			{
				GL11.glPushMatrix();
				GL11.glTranslated(x, y, 0);
				if(large)
					GL11.glScaled(1.5, 1.5, 1.5);
				else
					GL11.glScaled(0.75, 0.75, 0.75);
				
				DiffuseLighting.enable();
				mc.getItemRenderer()
					.renderGuiItem(new ItemStack(Blocks.GRASS_BLOCK), 0, 0);
				DiffuseLighting.disable();
				GL11.glPopMatrix();
				
				GL11.glPushMatrix();
				GL11.glTranslated(x, y, 0);
				if(large)
					GL11.glScaled(2, 2, 2);
				GL11.glDisable(GL11.GL_DEPTH_TEST);
				TextRenderer fr = mc.textRenderer;
				fr.drawWithShadow("?", 3, 2, 0xf0f0f0);
				GL11.glEnable(GL11.GL_DEPTH_TEST);
				GL11.glPopMatrix();
				
				return "\u00a7ounknown block\u00a7r";
				
			}else
			{
				GL11.glPushMatrix();
				GL11.glTranslated(x, y, 0);
				if(large)
					GL11.glScaled(1.5, 1.5, 1.5);
				else
					GL11.glScaled(0.75, 0.75, 0.75);
				
				DiffuseLighting.enable();
				mc.getItemRenderer().renderGuiItem(stack, 0, 0);
				DiffuseLighting.disable();
				
				GL11.glPopMatrix();
				
				return stack.getName().asFormattedString();
			}
		}
	}
}
