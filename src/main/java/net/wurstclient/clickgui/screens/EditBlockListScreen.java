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

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;

public final class EditBlockListScreen extends Screen
{
	private final Screen prevScreen;
	private final BlockListSetting blockList;
	
	private ListGui listGui;
	private EditBox blockNameField;
	private Button addButton;
	private Button removeButton;
	private Button doneButton;
	
	private Block blockToAdd;
	
	public EditBlockListScreen(Screen prevScreen, BlockListSetting blockList)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		this.blockList = blockList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, blockList.getBlockNames());
		addWidget(listGui);
		
		blockNameField = new EditBox(minecraft.font, width / 2 - 152,
			height - 56, 150, 20, Component.literal(""));
		addWidget(blockNameField);
		blockNameField.setMaxLength(256);
		
		addRenderableWidget(
			addButton = Button.builder(Component.literal("Add"), b -> {
				blockList.add(blockToAdd);
				minecraft.setScreen(EditBlockListScreen.this);
			}).bounds(width / 2 - 2, height - 56, 30, 20).build());
		
		addRenderableWidget(removeButton =
			Button.builder(Component.literal("Remove Selected"), b -> {
				blockList
					.remove(blockList.indexOf(listGui.getSelectedBlockName()));
				minecraft.setScreen(EditBlockListScreen.this);
			}).bounds(width / 2 + 52, height - 56, 100, 20).build());
		
		addRenderableWidget(
			Button.builder(Component.literal("Reset to Defaults"),
				b -> minecraft.setScreen(new ConfirmScreen(b2 -> {
					if(b2)
						blockList.resetToDefaults();
					minecraft.setScreen(EditBlockListScreen.this);
				}, Component.literal("Reset to Defaults"),
					Component.literal("Are you sure?"))))
				.bounds(width - 108, 8, 100, 20).build());
		
		addRenderableWidget(doneButton = Button
			.builder(Component.literal("Done"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
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
		String nameOrId = blockNameField.getValue();
		blockToAdd = BlockUtils.getBlockFromNameOrID(nameOrId);
		addButton.active = blockToAdd != null;
		
		removeButton.active = listGui.getSelected() != null;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		PoseStack matrixStack = context.pose();
		renderBackground(context, mouseX, mouseY, partialTicks);
		
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(minecraft.font,
			blockList.getName() + " (" + blockList.size() + ")", width / 2, 12,
			0xFFFFFF);
		
		matrixStack.pushPose();
		matrixStack.translate(0, 0, 300);
		
		blockNameField.render(context, mouseX, mouseY, partialTicks);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		matrixStack.pushPose();
		matrixStack.translate(-64 + width / 2 - 152, 0, 0);
		
		if(blockNameField.getValue().isEmpty() && !blockNameField.isFocused())
			context.drawString(minecraft.font, "block name or ID", 68,
				height - 50, 0x808080);
		
		int border = blockNameField.isFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;
		int black = 0xFF000000;
		
		context.fill(48, height - 56, 64, height - 36, border);
		context.fill(49, height - 55, 65, height - 37, black);
		context.fill(214, height - 56, 244, height - 55, border);
		context.fill(214, height - 37, 244, height - 36, border);
		context.fill(244, height - 56, 246, height - 36, border);
		context.fill(213, height - 55, 243, height - 52, black);
		context.fill(213, height - 40, 243, height - 37, black);
		context.fill(213, height - 55, 216, height - 37, black);
		context.fill(242, height - 55, 245, height - 37, black);
		
		matrixStack.popPose();
		
		RenderUtils.drawItem(context,
			blockToAdd == null ? ItemStack.EMPTY : new ItemStack(blockToAdd),
			width / 2 - 164, height - 52, false);
		
		matrixStack.popPose();
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
	
	private final class Entry
		extends ObjectSelectionList.Entry<EditBlockListScreen.Entry>
	{
		private final String blockName;
		
		public Entry(String blockName)
		{
			this.blockName = Objects.requireNonNull(blockName);
		}
		
		@Override
		public Component getNarration()
		{
			Block block = BlockUtils.getBlockFromName(blockName);
			ItemStack stack = new ItemStack(block);
			
			return Component.translatable("narrator.select",
				"Block " + getDisplayName(stack) + ", " + blockName + ", "
					+ getIdText(block));
		}
		
		@Override
		public void render(GuiGraphics context, int index, int y, int x,
			int entryWidth, int entryHeight, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			Block block = BlockUtils.getBlockFromName(blockName);
			ItemStack stack = new ItemStack(block);
			Font tr = minecraft.font;
			
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			context.drawString(tr, getDisplayName(stack), x + 28, y, 0xF0F0F0,
				false);
			context.drawString(tr, blockName, x + 28, y + 9, 0xA0A0A0, false);
			context.drawString(tr, getIdText(block), x + 28, y + 18, 0xA0A0A0,
				false);
		}
		
		private String getDisplayName(ItemStack stack)
		{
			return stack.isEmpty() ? "\u00a7ounknown block\u00a7r"
				: stack.getHoverName().getString();
		}
		
		private String getIdText(Block block)
		{
			return "ID: " + Block.getId(block.defaultBlockState());
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<EditBlockListScreen.Entry>
	{
		public ListGui(Minecraft minecraft, EditBlockListScreen screen,
			List<String> list)
		{
			super(minecraft, screen.width, screen.height - 96, 36, 30, 0);
			
			list.stream().map(EditBlockListScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public String getSelectedBlockName()
		{
			EditBlockListScreen.Entry selected = getSelected();
			return selected != null ? selected.blockName : null;
		}
	}
}
