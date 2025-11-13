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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import net.wurstclient.WurstClient;
import net.wurstclient.util.WurstColors;
import net.wurstclient.util.json.JsonException;

public final class KeybindProfilesScreen extends Screen
{
	private final Screen prevScreen;
	
	private ListGui listGui;
	private Button loadButton;
	
	public KeybindProfilesScreen(Screen prevScreen)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this,
			WurstClient.INSTANCE.getKeybinds().listProfiles());
		addWidget(listGui);
		
		addRenderableWidget(
			Button.builder(Component.literal("Open Folder"), b -> openFolder())
				.bounds(8, 8, 100, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("New Profile"),
				b -> minecraft.setScreen(
					new EnterProfileNameScreen(this, this::newProfile)))
			.bounds(width / 2 - 154, height - 48, 100, 20).build());
		
		loadButton = addRenderableWidget(
			Button.builder(Component.literal("Load"), b -> loadSelected())
				.bounds(width / 2 - 50, height - 48, 100, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Cancel"),
				b -> minecraft.setScreen(prevScreen))
			.bounds(width / 2 + 54, height - 48, 100, 20).build());
	}
	
	private void openFolder()
	{
		Util.getPlatform().openFile(
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
			minecraft.setScreen(prevScreen);
			return;
		}
		
		try
		{
			String fileName = "" + path.getFileName();
			WurstClient.INSTANCE.getKeybinds().loadProfile(fileName);
			minecraft.setScreen(prevScreen);
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		if(context.key() == GLFW.GLFW_KEY_ENTER)
			loadSelected();
		else if(context.key() == GLFW.GLFW_KEY_ESCAPE)
			minecraft.setScreen(prevScreen);
		
		return super.keyPressed(context);
	}
	
	@Override
	public void tick()
	{
		loadButton.active = listGui.getSelected() != null;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(minecraft.font, "Keybind Profiles",
			width / 2, 12, CommonColors.WHITE);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		if(loadButton.isHoveredOrFocused() && !loadButton.active)
			context.setComponentTooltipForNextFrame(font,
				Arrays
					.asList(Component.literal("You must first select a file.")),
				mouseX, mouseY);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private final class Entry
		extends ObjectSelectionList.Entry<KeybindProfilesScreen.Entry>
	{
		private final Path path;
		
		public Entry(Path path)
		{
			this.path = Objects.requireNonNull(path);
		}
		
		@Override
		public Component getNarration()
		{
			return Component.translatable("narrator.select",
				"Profile " + path.getFileName());
		}
		
		@Override
		public void renderContent(GuiGraphics context, int mouseX, int mouseY,
			boolean hovered, float tickDelta)
		{
			int x = getContentX();
			int y = getContentY();
			
			Font tr = minecraft.font;
			
			String fileName = "" + path.getFileName();
			context.drawString(tr, fileName, x + 28, y,
				WurstColors.VERY_LIGHT_GRAY);
			
			String relPath =
				"" + minecraft.gameDirectory.toPath().relativize(path);
			context.drawString(tr, relPath, x + 28, y + 9,
				CommonColors.LIGHT_GRAY);
		}
	}
	
	private final class ListGui
		extends ObjectSelectionList<KeybindProfilesScreen.Entry>
	{
		public ListGui(Minecraft mc, KeybindProfilesScreen screen,
			List<Path> list)
		{
			super(mc, screen.width, screen.height - 96, 36, 20);
			
			list.stream().map(KeybindProfilesScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public Path getSelectedPath()
		{
			KeybindProfilesScreen.Entry selected = getSelected();
			return selected != null ? selected.path : null;
		}
	}
}
