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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.wurstclient.WurstClient;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.util.WurstColors;

public final class KeybindManagerScreen extends Screen
{
	private final Screen prevScreen;
	
	private ListGui listGui;
	private ButtonWidget addButton;
	private ButtonWidget editButton;
	private ButtonWidget removeButton;
	private ButtonWidget backButton;
	
	public KeybindManagerScreen(Screen prevScreen)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this);
		addSelectableChild(listGui);
		
		addDrawableChild(addButton = ButtonWidget
			.builder(Text.literal("Add"),
				b -> client.setScreen(new KeybindEditorScreen(this)))
			.dimensions(width / 2 - 102, height - 52, 100, 20).build());
		
		addDrawableChild(
			editButton = ButtonWidget.builder(Text.literal("Edit"), b -> edit())
				.dimensions(width / 2 + 2, height - 52, 100, 20).build());
		
		addDrawableChild(removeButton =
			ButtonWidget.builder(Text.literal("Remove"), b -> remove())
				.dimensions(width / 2 - 102, height - 28, 100, 20).build());
		
		addDrawableChild(backButton = ButtonWidget
			.builder(Text.literal("Back"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 + 2, height - 28, 100, 20).build());
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset Keybinds"),
			b -> client.setScreen(new ConfirmScreen(confirmed -> {
				if(confirmed)
					WurstClient.INSTANCE.getKeybinds()
						.setKeybinds(KeybindList.DEFAULT_KEYBINDS);
				client.setScreen(this);
			}, Text.literal("Are you sure you want to reset your keybinds?"),
				Text.literal("This cannot be undone!"))))
			.dimensions(8, 8, 100, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Profiles..."),
				b -> client.setScreen(new KeybindProfilesScreen(this)))
			.dimensions(width - 108, 8, 100, 20).build());
	}
	
	private void edit()
	{
		Keybind keybind = listGui.getSelectedKeybind();
		if(keybind == null)
			return;
		
		client.setScreen(new KeybindEditorScreen(this, keybind.getKey(),
			keybind.getCommands()));
	}
	
	private void remove()
	{
		Keybind keybind = listGui.getSelectedKeybind();
		if(keybind == null)
			return;
		
		WurstClient.INSTANCE.getKeybinds().remove(keybind.getKey());
		client.setScreen(this);
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
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
		boolean selected = listGui.getSelectedOrNull() != null;
		editButton.active = selected;
		removeButton.active = selected;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(textRenderer, "Keybind Manager",
			width / 2, 8, Colors.WHITE);
		
		int count = WurstClient.INSTANCE.getKeybinds().getAllKeybinds().size();
		context.drawCenteredTextWithShadow(textRenderer, "Keybinds: " + count,
			width / 2, 20, Colors.WHITE);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private final class Entry
		extends AlwaysSelectedEntryListWidget.Entry<KeybindManagerScreen.Entry>
	{
		private final Keybind keybind;
		
		public Entry(Keybind keybind)
		{
			this.keybind = Objects.requireNonNull(keybind);
		}
		
		@Override
		public Text getNarration()
		{
			return Text.translatable("narrator.select", "Keybind " + keybind);
		}
		
		@Override
		public void render(DrawContext context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			TextRenderer tr = client.textRenderer;
			
			String keyText =
				"Key: " + keybind.getKey().replace("key.keyboard.", "");
			context.drawText(tr, keyText, x + 3, y + 3,
				WurstColors.VERY_LIGHT_GRAY, false);
			
			String cmdText = "Commands: " + keybind.getCommands();
			context.drawText(tr, cmdText, x + 3, y + 15, Colors.LIGHT_GRAY,
				false);
		}
	}
	
	private final class ListGui
		extends AlwaysSelectedEntryListWidget<KeybindManagerScreen.Entry>
	{
		public ListGui(MinecraftClient mc, KeybindManagerScreen screen)
		{
			super(mc, screen.width, screen.height - 96, 36, 30);
			
			WurstClient.INSTANCE.getKeybinds().getAllKeybinds().stream()
				.map(KeybindManagerScreen.Entry::new).forEach(this::addEntry);
		}
		
		public Keybind getSelectedKeybind()
		{
			KeybindManagerScreen.Entry selected = getSelectedOrNull();
			return selected != null ? selected.keybind : null;
		}
	}
}
