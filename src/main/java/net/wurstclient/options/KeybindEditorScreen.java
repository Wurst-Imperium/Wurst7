/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;

public final class KeybindEditorScreen extends Screen
	implements PressAKeyCallback
{
	private final Screen prevScreen;
	
	private String key;
	private final String oldKey;
	private final String oldCommands;
	
	private TextFieldWidget commandField;
	
	public KeybindEditorScreen(Screen prevScreen)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		
		key = "NONE";
		oldKey = null;
		oldCommands = null;
	}
	
	public KeybindEditorScreen(Screen prevScreen, String key, String commands)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		
		this.key = key;
		oldKey = key;
		oldCommands = commands;
	}
	
	@Override
	public void init()
	{
		addButton(new ButtonWidget(width / 2 - 100, 60, 200, 20, "Change Key",
			b -> minecraft.openScreen(new PressAKeyScreen(this))));
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 72, 200, 20,
			"Save", b -> save()));
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 96, 200, 20,
			"Cancel", b -> minecraft.openScreen(prevScreen)));
		
		commandField =
			new TextFieldWidget(font, width / 2 - 100, 100, 200, 20, "");
		commandField.setMaxLength(65536);
		children.add(commandField);
		setInitialFocus(commandField);
		commandField.setSelected(true);
		
		if(oldCommands != null)
			commandField.setText(oldCommands);
	}
	
	private void save()
	{
		if(oldKey != null)
			WurstClient.INSTANCE.getKeybinds().remove(oldKey);
		
		WurstClient.INSTANCE.getKeybinds().add(key, commandField.getText());
		minecraft.openScreen(prevScreen);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		commandField.mouseClicked(mouseX, mouseY, mouseButton);
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	public void tick()
	{
		commandField.tick();
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		renderBackground();
		
		drawCenteredString(font, (oldKey != null ? "Edit" : "Add") + " Keybind",
			width / 2, 20, 0xffffff);
		
		drawString(font, "Key: " + key.replace("key.keyboard.", ""),
			width / 2 - 100, 47, 0xa0a0a0);
		drawString(font, "Commands (separated by ';')", width / 2 - 100, 87,
			0xa0a0a0);
		
		commandField.render(mouseX, mouseY, partialTicks);
		super.render(mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void setKey(String key)
	{
		this.key = key;
	}
}
