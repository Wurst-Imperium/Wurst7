/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Util;
import net.wurstclient.WurstClient;
import net.wurstclient.util.WurstColors;
import net.wurstclient.util.json.JsonException;

public final class KeybindProfilesScreen extends Screen
{
	private final Screen prevScreen;
	
	private ListGui listGui;
	private ButtonWidget loadButton;
	
	public KeybindProfilesScreen(Screen prevScreen)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this,
			WurstClient.INSTANCE.getKeybinds().listProfiles());
		addSelectableChild(listGui);
		
		addDrawableChild(
			ButtonWidget.builder(Text.literal("Open Folder"), b -> openFolder())
				.dimensions(8, 8, 100, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("New Profile"),
				b -> client.setScreen(
					new EnterProfileNameScreen(this, this::newProfile)))
			.dimensions(width / 2 - 154, height - 48, 100, 20).build());
		
		loadButton = addDrawableChild(
			ButtonWidget.builder(Text.literal("Load"), b -> loadSelected())
				.dimensions(width / 2 - 50, height - 48, 100, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Cancel"), b -> client.setScreen(prevScreen))
			.dimensions(width / 2 + 54, height - 48, 100, 20).build());
	}
	
	private void openFolder()
	{
		Util.getOperatingSystem().open(
			WurstClient.INSTANCE.getKeybinds().getProfilesFolder().toFile());
	}
	
	private void newProfile(String name)
	{
		if(!name.endsWith(".json"))
			name += ".json";
		
		try
		{
			WurstClient.INSTANCE.getKeybinds().saveProfile(name);
			
		}catch(IOException | JsonException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private void loadSelected()
	{
		Path path = listGui.getSelectedPath();
		if(path == null)
		{
			client.setScreen(prevScreen);
			return;
		}
		
		try
		{
			String fileName = "" + path.getFileName();
			WurstClient.INSTANCE.getKeybinds().loadProfile(fileName);
			client.setScreen(prevScreen);
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
	{
		if(context.key() == GLFW.GLFW_KEY_ENTER)
			loadSelected();
		else if(context.key() == GLFW.GLFW_KEY_ESCAPE)
			client.setScreen(prevScreen);
		
		return super.keyPressed(context);
	}
	
	@Override
	public void tick()
	{
		loadButton.active = listGui.getSelectedOrNull() != null;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(client.textRenderer,
			"Keybind Profiles", width / 2, 12, Colors.WHITE);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		if(loadButton.isSelected() && !loadButton.active)
			context.drawTooltip(textRenderer,
				Arrays.asList(Text.literal("You must first select a file.")),
				mouseX, mouseY);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private final class Entry
		extends AlwaysSelectedEntryListWidget.Entry<KeybindProfilesScreen.Entry>
	{
		private final Path path;
		
		public Entry(Path path)
		{
			this.path = Objects.requireNonNull(path);
		}
		
		@Override
		public Text getNarration()
		{
			return Text.translatable("narrator.select",
				"Profile " + path.getFileName());
		}
		
		@Override
		public void render(DrawContext context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			TextRenderer tr = client.textRenderer;
			
			String fileName = "" + path.getFileName();
			context.drawTextWithShadow(tr, fileName, x + 28, y,
				WurstColors.VERY_LIGHT_GRAY);
			
			String relPath = "" + client.runDirectory.toPath().relativize(path);
			context.drawTextWithShadow(tr, relPath, x + 28, y + 9,
				Colors.LIGHT_GRAY);
		}
	}
	
	private final class ListGui
		extends AlwaysSelectedEntryListWidget<KeybindProfilesScreen.Entry>
	{
		public ListGui(MinecraftClient mc, KeybindProfilesScreen screen,
			List<Path> list)
		{
			super(mc, screen.width, screen.height - 96, 36, 20);
			
			list.stream().map(KeybindProfilesScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public Path getSelectedPath()
		{
			KeybindProfilesScreen.Entry selected = getSelectedOrNull();
			return selected != null ? selected.path : null;
		}
	}
}
