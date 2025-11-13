/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.util.Objects;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.wurstclient.WurstClient;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.util.WurstColors;

public final class KeybindManagerScreen extends Screen
{
	private final Screen prevScreen;
	
	private ListGui listGui;
	private Button addButton;
	private Button editButton;
	private Button removeButton;
	private Button backButton;
	
	public KeybindManagerScreen(Screen prevScreen)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this);
		addWidget(listGui);
		
		addRenderableWidget(addButton = Button
			.builder(Component.literal("Add"),
				b -> minecraft.setScreen(new KeybindEditorScreen(this)))
			.bounds(width / 2 - 102, height - 52, 100, 20).build());
		
		addRenderableWidget(
			editButton = Button.builder(Component.literal("Edit"), b -> edit())
				.bounds(width / 2 + 2, height - 52, 100, 20).build());
		
		addRenderableWidget(removeButton =
			Button.builder(Component.literal("Remove"), b -> remove())
				.bounds(width / 2 - 102, height - 28, 100, 20).build());
		
		addRenderableWidget(backButton = Button
			.builder(Component.literal("Back"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 + 2, height - 28, 100, 20).build());
		
		addRenderableWidget(Button.builder(Component.literal("Reset Keybinds"),
			b -> minecraft.setScreen(new ConfirmScreen(confirmed -> {
				if(confirmed)
					WurstClient.INSTANCE.getKeybinds()
						.setKeybinds(KeybindList.DEFAULT_KEYBINDS);
				minecraft.setScreen(this);
			}, Component
				.literal("Are you sure you want to reset your keybinds?"),
				Component.literal("This cannot be undone!"))))
			.bounds(8, 8, 100, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Profiles..."),
				b -> minecraft.setScreen(new KeybindProfilesScreen(this)))
			.bounds(width - 108, 8, 100, 20).build());
	}
	
	private void edit()
	{
		Keybind keybind = listGui.getSelectedKeybind();
		if(keybind == null)
			return;
		
		minecraft.setScreen(new KeybindEditorScreen(this, keybind.getKey(),
			keybind.getCommands()));
	}
	
	private void remove()
	{
		Keybind keybind = listGui.getSelectedKeybind();
		if(keybind == null)
			return;
		
		WurstClient.INSTANCE.getKeybinds().remove(keybind.getKey());
		minecraft.setScreen(this);
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			if(editButton.active)
				editButton.onPress(context);
			else
				addButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_DELETE:
			removeButton.onPress(context);
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			backButton.onPress(context);
			break;
			
			default:
			break;
		}
		
		return super.keyPressed(context);
	}
	
	@Override
	public void tick()
	{
		boolean selected = listGui.getSelected() != null;
		editButton.active = selected;
		removeButton.active = selected;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(font, "Keybind Manager", width / 2, 8,
			CommonColors.WHITE);
		
		int count = WurstClient.INSTANCE.getKeybinds().getAllKeybinds().size();
		context.drawCenteredString(font, "Keybinds: " + count, width / 2, 20,
			CommonColors.WHITE);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private final class Entry
		extends ObjectSelectionList.Entry<KeybindManagerScreen.Entry>
	{
		private final Keybind keybind;
		
		public Entry(Keybind keybind)
		{
			this.keybind = Objects.requireNonNull(keybind);
		}
		
		@Override
		public Component getNarration()
		{
			return Component.translatable("narrator.select",
				"Keybind " + keybind);
		}
		
		@Override
		public void renderContent(GuiGraphics context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			Font tr = minecraft.font;
			
			String keyText =
				"Key: " + keybind.getKey().replace("key.keyboard.", "");
			context.drawString(tr, keyText, x + 3, y + 3,
				WurstColors.VERY_LIGHT_GRAY, false);
			
			String cmdText = "Commands: " + keybind.getCommands();
			context.drawString(tr, cmdText, x + 3, y + 15,
				CommonColors.LIGHT_GRAY, false);
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<KeybindManagerScreen.Entry>
	{
		public ListGui(Minecraft mc, KeybindManagerScreen screen)
		{
			super(mc, screen.width, screen.height - 96, 36, 30);
			
			WurstClient.INSTANCE.getKeybinds().getAllKeybinds().stream()
				.map(KeybindManagerScreen.Entry::new).forEach(this::addEntry);
		}
		
		public Keybind getSelectedKeybind()
		{
			KeybindManagerScreen.Entry selected = getSelected();
			return selected != null ? selected.keybind : null;
		}
	}
}
