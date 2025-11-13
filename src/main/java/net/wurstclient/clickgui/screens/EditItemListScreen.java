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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.util.ItemUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

public final class EditItemListScreen extends Screen
{
	private final Screen prevScreen;
	private final ItemListSetting itemList;
	
	private ListGui listGui;
	private EditBox itemNameField;
	private Button addButton;
	private Button removeButton;
	private Button doneButton;
	
	private Item itemToAdd;
	
	public EditItemListScreen(Screen prevScreen, ItemListSetting itemList)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		this.itemList = itemList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, itemList.getItemNames());
		addWidget(listGui);
		
		itemNameField = new EditBox(minecraft.font, width / 2 - 152,
			height - 56, 150, 20, Component.literal(""));
		addWidget(itemNameField);
		itemNameField.setMaxLength(256);
		
		addRenderableWidget(
			addButton = Button.builder(Component.literal("Add"), b -> {
				itemList.add(itemToAdd);
				minecraft.setScreen(EditItemListScreen.this);
			}).bounds(width / 2 - 2, height - 56, 30, 20).build());
		
		addRenderableWidget(removeButton =
			Button.builder(Component.literal("Remove Selected"), b -> {
				itemList.remove(itemList.getItemNames()
					.indexOf(listGui.getSelectedBlockName()));
				minecraft.setScreen(EditItemListScreen.this);
			}).bounds(width / 2 + 52, height - 56, 100, 20).build());
		
		addRenderableWidget(
			Button.builder(Component.literal("Reset to Defaults"),
				b -> minecraft.setScreen(new ConfirmScreen(b2 -> {
					if(b2)
						itemList.resetToDefaults();
					minecraft.setScreen(EditItemListScreen.this);
				}, Component.literal("Reset to Defaults"),
					Component.literal("Are you sure?"))))
				.bounds(width - 108, 8, 100, 20).build());
		
		addRenderableWidget(doneButton = Button
			.builder(Component.literal("Done"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent context, boolean doubleClick)
	{
		itemNameField.mouseClicked(context, doubleClick);
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			if(addButton.active)
				addButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			if(!itemNameField.isFocused())
				removeButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			doneButton.onPress(context);
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(context);
	}
	
	@Override
	public void tick()
	{
		String nameOrId = itemNameField.getValue().toLowerCase();
		itemToAdd = ItemUtils.getItemFromNameOrID(nameOrId);
		addButton.active = itemToAdd != null;
		
		removeButton.active = listGui.getSelected() != null;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		Matrix3x2fStack matrixStack = context.pose();
		
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(minecraft.font,
			itemList.getName() + " (" + itemList.getItemNames().size() + ")",
			width / 2, 12, CommonColors.WHITE);
		
		matrixStack.pushMatrix();
		
		itemNameField.render(context, mouseX, mouseY, partialTicks);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		context.guiRenderState.up();
		matrixStack.pushMatrix();
		matrixStack.translate(-64 + width / 2 - 152, 0);
		
		if(itemNameField.getValue().isEmpty() && !itemNameField.isFocused())
			context.drawString(minecraft.font, "item name or ID", 68,
				height - 50, CommonColors.GRAY);
		
		int border = itemNameField.isFocused() ? CommonColors.WHITE
			: CommonColors.LIGHT_GRAY;
		int black = CommonColors.BLACK;
		
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
			itemToAdd == null ? ItemStack.EMPTY : new ItemStack(itemToAdd),
			width / 2 - 164, height - 52, false);
		
		matrixStack.popMatrix();
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
		extends ObjectSelectionList.Entry<EditItemListScreen.Entry>
	{
		private final String itemName;
		
		public Entry(String itemName)
		{
			this.itemName = Objects.requireNonNull(itemName);
		}
		
		@Override
		public Component getNarration()
		{
			Item item =
				BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName));
			ItemStack stack = new ItemStack(item);
			
			return Component.translatable("narrator.select",
				"Item " + getDisplayName(stack) + ", " + itemName + ", "
					+ getIdText(item));
		}
		
		@Override
		public void renderContent(GuiGraphics context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			Item item =
				BuiltInRegistries.ITEM.getValue(Identifier.parse(itemName));
			ItemStack stack = new ItemStack(item);
			Font tr = minecraft.font;
			
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			context.drawString(tr, getDisplayName(stack), x + 28, y,
				WurstColors.VERY_LIGHT_GRAY, false);
			context.drawString(tr, itemName, x + 28, y + 9,
				CommonColors.LIGHT_GRAY, false);
			context.drawString(tr, getIdText(item), x + 28, y + 18,
				CommonColors.LIGHT_GRAY, false);
		}
		
		private String getDisplayName(ItemStack stack)
		{
			return stack.isEmpty() ? "\u00a7ounknown item\u00a7r"
				: stack.getHoverName().getString();
		}
		
		private String getIdText(Item item)
		{
			return "ID: " + BuiltInRegistries.ITEM.getId(item);
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<EditItemListScreen.Entry>
	{
		public ListGui(Minecraft minecraft, EditItemListScreen screen,
			List<String> list)
		{
			super(minecraft, screen.width, screen.height - 96, 36, 30);
			
			list.stream().map(EditItemListScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public String getSelectedBlockName()
		{
			EditItemListScreen.Entry selected = getSelected();
			return selected != null ? selected.itemName : null;
		}
	}
}
