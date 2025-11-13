/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.WurstColors;

public final class SelectFileScreen extends Screen
{
	private final Screen prevScreen;
	private final FileSetting setting;
	
	private ListGui listGui;
	private Button doneButton;
	
	public SelectFileScreen(Screen prevScreen, FileSetting blockList)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		setting = blockList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(minecraft, this, setting.listFiles());
		addWidget(listGui);
		
		addRenderableWidget(
			Button.builder(Component.literal("Open Folder"), b -> openFolder())
				.bounds(8, 8, 100, 20).build());
		
		addRenderableWidget(Button
			.builder(Component.literal("Reset to Defaults"),
				b -> askToConfirmReset())
			.bounds(width - 108, 8, 100, 20).build());
		
		doneButton = addRenderableWidget(
			Button.builder(Component.literal("Done"), b -> done())
				.bounds(width / 2 - 102, height - 48, 100, 20).build());
		
		addRenderableWidget(
			Button.builder(Component.literal("Cancel"), b -> openPrevScreen())
				.bounds(width / 2 + 2, height - 48, 100, 20).build());
	}
	
	private void openFolder()
	{
		Util.getPlatform().openFile(setting.getFolder().toFile());
	}
	
	private void openPrevScreen()
	{
		minecraft.setScreen(prevScreen);
	}
	
	private void done()
	{
		Path path = listGui.getSelectedPath();
		if(path != null)
		{
			String fileName = "" + path.getFileName();
			setting.setSelectedFile(fileName);
		}
		
		openPrevScreen();
	}
	
	private void askToConfirmReset()
	{
		Component title = Component.literal("Reset Folder");
		
		Component message = Component
			.literal("This will empty the '" + setting.getFolder().getFileName()
				+ "' folder and then re-generate the default files.\n"
				+ "Are you sure you want to do this?");
		
		minecraft
			.setScreen(new ConfirmScreen(this::confirmReset, title, message));
	}
	
	private void confirmReset(boolean confirmed)
	{
		if(confirmed)
			setting.resetFolder();
		
		minecraft.setScreen(SelectFileScreen.this);
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		if(context.key() == GLFW.GLFW_KEY_ENTER)
			done();
		else if(context.key() == GLFW.GLFW_KEY_ESCAPE)
			openPrevScreen();
		
		return super.keyPressed(context);
	}
	
	@Override
	public void tick()
	{
		doneButton.active = listGui.getSelected() != null;
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredString(minecraft.font, setting.getName(), width / 2,
			12, CommonColors.WHITE);
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		if(doneButton.isHoveredOrFocused() && !doneButton.active)
			context.setComponentTooltipForNextFrame(font,
				Arrays
					.asList(Component.literal("You must first select a file.")),
				mouseX, mouseY);
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
		extends ObjectSelectionList.Entry<SelectFileScreen.Entry>
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
				"File " + path.getFileName());
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
		extends ObjectSelectionList<SelectFileScreen.Entry>
	{
		public ListGui(Minecraft mc, SelectFileScreen screen, List<Path> list)
		{
			super(mc, screen.width, screen.height - 96, 36, 20);
			
			list.stream().map(SelectFileScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public Path getSelectedPath()
		{
			SelectFileScreen.Entry selected = getSelected();
			return selected != null ? selected.path : null;
		}
	}
}
