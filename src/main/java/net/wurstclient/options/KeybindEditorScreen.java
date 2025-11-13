/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.wurstclient.WurstClient;
import net.wurstclient.util.WurstColors;

public final class KeybindEditorScreen extends Screen
	implements PressAKeyCallback
{
	private final Screen prevScreen;
	
	private String key;
	private final String oldKey;
	private final String oldCommands;
	
	private EditBox commandField;
	
	public KeybindEditorScreen(Screen prevScreen)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		
		key = "NONE";
		oldKey = null;
		oldCommands = null;
	}
	
	public KeybindEditorScreen(Screen prevScreen, String key, String commands)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		
		this.key = key;
		oldKey = key;
		oldCommands = commands;
	}
	
	@Override
	public void init()
	{
		addRenderableWidget(Button
			.builder(Component.literal("Change Key"),
				b -> minecraft.setScreen(new PressAKeyScreen(this)))
			.bounds(width / 2 - 100, 60, 200, 20).build());
		
		addRenderableWidget(
			Button.builder(Component.literal("Save"), b -> save())
				.bounds(width / 2 - 100, height / 4 + 72, 200, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Cancel"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 - 100, height / 4 + 96, 200, 20).build());
		
		commandField = new EditBox(font, width / 2 - 100, 100, 200, 20,
			Component.literal(""));
		commandField.setMaxLength(65536);
		addWidget(commandField);
		setFocused(commandField);
		commandField.setFocused(true);
		
		if(oldCommands != null)
			commandField.setValue(oldCommands);
	}
	
	private void save()
	{
		if(oldKey != null)
			WurstClient.INSTANCE.getKeybinds().remove(oldKey);
		
		WurstClient.INSTANCE.getKeybinds().add(key, commandField.getValue());
		minecraft.setScreen(prevScreen);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent context, boolean doubleClick)
	{
		commandField.mouseClicked(context, doubleClick);
		return super.mouseClicked(context, doubleClick);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		context.drawCenteredString(font,
			(oldKey != null ? "Edit" : "Add") + " Keybind", width / 2, 20,
			CommonColors.WHITE);
		
		context.drawString(font, "Key: " + key.replace("key.keyboard.", ""),
			width / 2 - 100, 47, WurstColors.VERY_LIGHT_GRAY);
		context.drawString(font, "Commands (separated by ';')", width / 2 - 100,
			87, WurstColors.VERY_LIGHT_GRAY);
		
		commandField.render(context, mouseX, mouseY, partialTicks);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void onClose()
	{
		minecraft.setScreen(prevScreen);
	}
	
	@Override
	public void setKey(String key)
	{
		this.key = key;
	}
}
