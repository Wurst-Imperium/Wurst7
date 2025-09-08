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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.util.ItemUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.WurstColors;

public final class EditItemListScreen extends Screen
{
	private final Screen prevScreen;
	private final ItemListSetting itemList;
	
	private ListGui listGui;
	private TextFieldWidget itemNameField;
	private ButtonWidget addButton;
	private ButtonWidget removeButton;
	private ButtonWidget doneButton;
	
	private Item itemToAdd;
	
	public EditItemListScreen(Screen prevScreen, ItemListSetting itemList)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.itemList = itemList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, itemList.getItemNames());
		addSelectableChild(listGui);
		
		itemNameField = new TextFieldWidget(client.textRenderer,
			width / 2 - 152, height - 56, 150, 20, Text.literal(""));
		addSelectableChild(itemNameField);
		itemNameField.setMaxLength(256);
		
		addDrawableChild(
			addButton = ButtonWidget.builder(Text.literal("Add"), b -> {
				itemList.add(itemToAdd);
				client.setScreen(EditItemListScreen.this);
			}).dimensions(width / 2 - 2, height - 56, 30, 20).build());
		
		addDrawableChild(removeButton =
			ButtonWidget.builder(Text.literal("Remove Selected"), b -> {
				itemList.remove(itemList.getItemNames()
					.indexOf(listGui.getSelectedBlockName()));
				client.setScreen(EditItemListScreen.this);
			}).dimensions(width / 2 + 52, height - 56, 100, 20).build());
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Defaults"),
			b -> client.setScreen(new ConfirmScreen(b2 -> {
				if(b2)
					itemList.resetToDefaults();
				client.setScreen(EditItemListScreen.this);
			}, Text.literal("Reset to Defaults"),
				Text.literal("Are you sure?"))))
			.dimensions(width - 108, 8, 100, 20).build());
		
		addDrawableChild(doneButton = ButtonWidget
			.builder(Text.literal("Done"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	@Override
	public boolean mouseClicked(Click context, boolean doubleClick)
	{
		itemNameField.mouseClicked(context, doubleClick);
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
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
		String nameOrId = itemNameField.getText().toLowerCase();
		itemToAdd = ItemUtils.getItemFromNameOrID(nameOrId);
		addButton.active = itemToAdd != null;
		
		removeButton.active = listGui.getSelectedOrNull() != null;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		Matrix3x2fStack matrixStack = context.getMatrices();
		
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(client.textRenderer,
			itemList.getName() + " (" + itemList.getItemNames().size() + ")",
			width / 2, 12, Colors.WHITE);
		
		matrixStack.pushMatrix();
		
		itemNameField.render(context, mouseX, mouseY, partialTicks);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		context.state.goUpLayer();
		matrixStack.pushMatrix();
		matrixStack.translate(-64 + width / 2 - 152, 0);
		
		if(itemNameField.getText().isEmpty() && !itemNameField.isFocused())
			context.drawTextWithShadow(client.textRenderer, "item name or ID",
				68, height - 50, Colors.GRAY);
		
		int border =
			itemNameField.isFocused() ? Colors.WHITE : Colors.LIGHT_GRAY;
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
			itemToAdd == null ? ItemStack.EMPTY : new ItemStack(itemToAdd),
			width / 2 - 164, height - 52, false);
		
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
		extends AlwaysSelectedEntryListWidget.Entry<EditItemListScreen.Entry>
	{
		private final String itemName;
		
		public Entry(String itemName)
		{
			this.itemName = Objects.requireNonNull(itemName);
		}
		
		@Override
		public Text getNarration()
		{
			Item item = Registries.ITEM.get(Identifier.of(itemName));
			ItemStack stack = new ItemStack(item);
			
			return Text.translatable("narrator.select",
				"Item " + getDisplayName(stack) + ", " + itemName + ", "
					+ getIdText(item));
		}
		
		@Override
		public void render(DrawContext context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			Item item = Registries.ITEM.get(Identifier.of(itemName));
			ItemStack stack = new ItemStack(item);
			TextRenderer tr = client.textRenderer;
			
			RenderUtils.drawItem(context, stack, x + 1, y + 1, true);
			context.drawText(tr, getDisplayName(stack), x + 28, y,
				WurstColors.VERY_LIGHT_GRAY, false);
			context.drawText(tr, itemName, x + 28, y + 9, Colors.LIGHT_GRAY,
				false);
			context.drawText(tr, getIdText(item), x + 28, y + 18,
				Colors.LIGHT_GRAY, false);
		}
		
		private String getDisplayName(ItemStack stack)
		{
			return stack.isEmpty() ? "\u00a7ounknown item\u00a7r"
				: stack.getName().getString();
		}
		
		private String getIdText(Item item)
		{
			return "ID: " + Registries.ITEM.getRawId(item);
		}
	}
	
	private final class ListGui
		extends AlwaysSelectedEntryListWidget<EditItemListScreen.Entry>
	{
		public ListGui(MinecraftClient minecraft, EditItemListScreen screen,
			List<String> list)
		{
			super(minecraft, screen.width, screen.height - 96, 36, 30);
			
			list.stream().map(EditItemListScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public String getSelectedBlockName()
		{
			EditItemListScreen.Entry selected = getSelectedOrNull();
			return selected != null ? selected.itemName : null;
		}
	}
}
