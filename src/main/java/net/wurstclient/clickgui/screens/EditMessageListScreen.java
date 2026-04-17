/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.wurstclient.settings.MessageListSetting;
import net.wurstclient.util.WurstColors;

public final class EditMessageListScreen extends Screen
{
	private final Screen prevScreen;
	private final MessageListSetting messageList;
	
	private ListGui listGui;
	private EditBox messageField;
	private Button addButton;
	private Button editButton;
	private Button removeButton;
	private Button doneButton;
	
	private int editingIndex = -1;
	
	public EditMessageListScreen(Screen prevScreen,
		MessageListSetting messageList)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		this.messageList = messageList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, messageList.getMessages());
		addWidget(listGui);
		
		int fieldWidth = 260;
		int fieldX = width / 2 - fieldWidth / 2;
		messageField = new EditBox(minecraft.font, fieldX, height - 56,
			fieldWidth, 20, Component.literal(""));
		addWidget(messageField);
		messageField.setMaxLength(256);
		
		addRenderableWidget(
			addButton = Button.builder(Component.literal("Add"), b -> {
				commitAdd();
			}).bounds(width / 2 + fieldWidth / 2 + 4, height - 56, 40, 20)
				.build());
		
		addRenderableWidget(
			editButton = Button.builder(Component.literal("Edit"), b -> {
				ListEntry selected = listGui.getSelected();
				if(selected == null)
					return;
				editingIndex = selected.index;
				messageField.setValue(selected.message);
				messageField.setFocused(true);
			}).bounds(width / 2 + fieldWidth / 2 + 48, height - 56, 40, 20)
				.build());
		
		addRenderableWidget(
			removeButton = Button.builder(Component.literal("Remove"), b -> {
				ListEntry selected = listGui.getSelected();
				if(selected != null)
					messageList.remove(selected.index);
				editingIndex = -1;
				messageField.setValue("");
				minecraft.setScreen(EditMessageListScreen.this);
			}).bounds(width / 2 + fieldWidth / 2 + 92, height - 56, 60, 20)
				.build());
		
		addRenderableWidget(
			Button.builder(Component.literal("Reset to Defaults"),
				b -> minecraft.setScreen(new ConfirmScreen(b2 -> {
					if(b2)
						messageList.resetToDefaults();
					minecraft.setScreen(EditMessageListScreen.this);
				}, Component.literal("Reset to Defaults"),
					Component.literal("Are you sure?"))))
				.bounds(width - 108, 8, 100, 20).build());
		
		addRenderableWidget(doneButton = Button
			.builder(Component.literal("Done"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height - 28, 200, 20).build());
	}
	
	private void commitAdd()
	{
		String value = messageField.getValue();
		if(value.isBlank())
			return;
		
		if(editingIndex >= 0)
		{
			messageList.remove(editingIndex);
			messageList.addAt(editingIndex, value);
			editingIndex = -1;
		}else
		{
			messageList.add(value);
		}
		
		messageField.setValue("");
		minecraft.setScreen(EditMessageListScreen.this);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent context, boolean doubleClick)
	{
		messageField.mouseClicked(context, doubleClick);
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			if(addButton.active)
				commitAdd();
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			if(!messageField.isFocused())
				removeButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			if(editingIndex >= 0)
			{
				editingIndex = -1;
				messageField.setValue("");
			}else
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
		addButton.active = !messageField.getValue().isBlank();
		boolean hasSelection = listGui.getSelected() != null;
		editButton.active = hasSelection;
		removeButton.active = hasSelection;
		
		addButton
			.setMessage(Component.literal(editingIndex >= 0 ? "Save" : "Add"));
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX,
		int mouseY, float partialTicks)
	{
		Matrix3x2fStack matrixStack = context.pose();
		
		listGui.extractRenderState(context, mouseX, mouseY, partialTicks);
		
		context.centeredText(minecraft.font,
			messageList.getName() + " (" + messageList.size() + ")", width / 2,
			12, CommonColors.WHITE);
		
		matrixStack.pushMatrix();
		
		messageField.extractRenderState(context, mouseX, mouseY, partialTicks);
		
		for(Renderable drawable : renderables)
			drawable.extractRenderState(context, mouseX, mouseY, partialTicks);
		
		if(messageField.getValue().isEmpty() && !messageField.isFocused())
		{
			context.text(minecraft.font,
				editingIndex >= 0 ? "Editing message..." : "New message...",
				messageField.getX() + 4, height - 50, CommonColors.GRAY, false);
		}
		
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
	
	private final class ListEntry
		extends ObjectSelectionList.Entry<EditMessageListScreen.ListEntry>
	{
		private final String message;
		private final int index;
		
		public ListEntry(String message, int index)
		{
			this.message = Objects.requireNonNull(message);
			this.index = index;
		}
		
		@Override
		public Component getNarration()
		{
			return Component.translatable("narrator.select", message);
		}
		
		@Override
		public void extractContent(GuiGraphicsExtractor context, int mouseX,
			int mouseY, boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			String display =
				minecraft.font.plainSubstrByWidth(message, getWidth() - 12);
			
			int color = message.startsWith("/") ? CommonColors.LIGHT_GRAY
				: WurstColors.VERY_LIGHT_GRAY;
			
			context.text(minecraft.font, display, x + 4, y + 4, color, false);
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<EditMessageListScreen.ListEntry>
	{
		public ListGui(Minecraft minecraft, EditMessageListScreen screen,
			List<String> list)
		{
			super(minecraft, screen.width, screen.height - 96, 36, 22);
			
			for(int i = 0; i < list.size(); i++)
				addEntry(new ListEntry(list.get(i), i));
		}
	}
}
