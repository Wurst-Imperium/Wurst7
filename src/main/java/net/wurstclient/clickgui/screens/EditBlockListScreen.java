/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.util.List;
import java.util.Objects;

import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

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
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.blockList = blockList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, blockList.getBlockNames());
		addSelectableChild(listGui);
		
		blockNameField = new TextFieldWidget(client.textRenderer,
			width / 2 - 152, height - 56, 150, 20, Text.literal(""));
		addSelectableChild(blockNameField);
		blockNameField.setMaxLength(256);
		
		addDrawableChild(
			addButton = ButtonWidget.builder(Text.literal("Add"), b -> {
				blockList.add(blockToAdd);
				client.setScreen(EditBlockListScreen.this);
			}).dimensions(width / 2 - 2, height - 56, 30, 20).build());
		
		addDrawableChild(removeButton =
			ButtonWidget.builder(Text.literal("Remove Selected"), b -> {
				blockList
					.remove(blockList.indexOf(listGui.getSelectedBlockName()));
				client.setScreen(EditBlockListScreen.this);
			}).dimensions(width / 2 + 52, height - 56, 100, 20).build());
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Defaults"),
			b -> client.setScreen(new ConfirmScreen(b2 -> {
				if(b2)
					blockList.resetToDefaults();
				client.setScreen(EditBlockListScreen.this);
			}, Text.literal("Reset to Defaults"),
				Text.literal("Are you sure?"))))
			.dimensions(width - 108, 8, 100, 20).build());
		
		addDrawableChild(doneButton = ButtonWidget
			.builder(Text.literal("Done"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		blockNameField.mouseClicked(mouseX, mouseY, mouseButton);
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			if(addButton.active)
				addButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			if(!blockNameField.isFocused())
				removeButton.onPress();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			doneButton.onPress();
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		String nameOrId = blockNameField.getText();
		blockToAdd = BlockUtils.getBlockFromNameOrID(nameOrId);
		addButton.active = blockToAdd != null;
		
		removeButton.active = listGui.getSelectedOrNull() != null;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		Matrix3x2fStack matrixStack = context.getMatrices();
		
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(client.textRenderer,
			blockList.getName() + " (" + blockList.size() + ")", width / 2, 12,
			Colors.WHITE);
		
		matrixStack.pushMatrix();
		
		blockNameField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		context.state.goUpLayer();
		matrixStack.pushMatrix();
		matrixStack.translate(-64 + width / 2 - 152, 0);
		
		if(blockNameField.getText().isEmpty() && !blockNameField.isFocused())
			context.drawTextWithShadow(client.textRenderer, "block name or ID",
				68, height - 50, Colors.GRAY);
		
		int border =
			blockNameField.isFocused() ? Colors.WHITE : Colors.LIGHT_GRAY;
		int black = Colors.BLACK;
		
		context.fill(48, height - 56, 64, height - 36, border);
		context.fill(49, height - 55, 65, height - 37, black);
		context.fill(214, height - 56, 244, height - 55, border);
		context.fill(214, height - 37, 244, height - 36, border);
		context.fill(244, height - 56, 246, height - 36, border);
		context.fill(213, height - 55, 243, height - 52, black);
		context.fill(213, height - 40, 243, height - 37, black);
		context.fill(213, height - 55, 216, height - 37, black);
		context.fill(242, height - 55, 245, height - 37, black);
		
		matrixStack.popMatrix();
		
		RenderUtils.drawItem(context,
			blockToAdd == null ? ItemStack.EMPTY : new ItemStack(blockToAdd),
			width / 2 - 164, height - 52, false);
		
		context.state.goDownLayer();
		matrixStack.popMatrix();
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
	
	private final class Entry
		extends AlwaysSelectedEntryListWidget.Entry<EditBlockListScreen.Entry>
	{
		private final String blockName;
		
		public Entry(String blockName)
		{
			this.blockName = Objects.requireNonNull(blockName);
		}
		
		@Override
		public Text getNarration()
		{
			Block block = BlockUtils.getBlockFromName(blockName);
			ItemStack stack = new ItemStack(block);
			
			return Text.translatable("narrator.select",
				"Block " + getDisplayName(stack) + ", " + blockName + ", "
					+ getIdText(block));
		}
		
		@Override
		public void render(DrawContext context, int index, int y, int x,
			int entryWidth, int entryHeight, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			Block block = BlockUtils.getBlockFromName(blockName);
			ItemStack stack = new ItemStack(block);
			TextRenderer tr = client.textRenderer;
			
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			context.drawText(tr, getDisplayName(stack), x + 28, y,
				WurstColors.VERY_LIGHT_GRAY, false);
			context.drawText(tr, blockName, x + 28, y + 9, Colors.LIGHT_GRAY,
				false);
			context.drawText(tr, getIdText(block), x + 28, y + 18,
				Colors.LIGHT_GRAY, false);
		}
		
		private String getDisplayName(ItemStack stack)
		{
			return stack.isEmpty() ? "\u00a7ounknown block\u00a7r"
				: stack.getName().getString();
		}
		
		private String getIdText(Block block)
		{
			return "ID: " + Block.getRawIdFromState(block.getDefaultState());
		}
	}
	
	private final class ListGui
		extends AlwaysSelectedEntryListWidget<EditBlockListScreen.Entry>
	{
		public ListGui(MinecraftClient minecraft, EditBlockListScreen screen,
			List<String> list)
		{
			super(minecraft, screen.width, screen.height - 96, 36, 30, 0);
			
			list.stream().map(EditBlockListScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public String getSelectedBlockName()
		{
			EditBlockListScreen.Entry selected = getSelectedOrNull();
			return selected != null ? selected.blockName : null;
		}
	}
}
