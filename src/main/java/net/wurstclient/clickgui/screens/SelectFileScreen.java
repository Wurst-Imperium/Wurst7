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
import net.minecraft.util.Util;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.WurstColors;

public final class SelectFileScreen extends Screen
{
	private final Screen prevScreen;
	private final FileSetting setting;
	
	private ListGui listGui;
	private ButtonWidget doneButton;
	
	public SelectFileScreen(Screen prevScreen, FileSetting blockList)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		setting = blockList;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this, setting.listFiles());
		addSelectableChild(listGui);
		
		addDrawableChild(
			ButtonWidget.builder(Text.literal("Open Folder"), b -> openFolder())
				.dimensions(8, 8, 100, 20).build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Reset to Defaults"),
				b -> askToConfirmReset())
			.dimensions(width - 108, 8, 100, 20).build());
		
		doneButton = addDrawableChild(
			ButtonWidget.builder(Text.literal("Done"), b -> done())
				.dimensions(width / 2 - 102, height - 48, 100, 20).build());
		
		addDrawableChild(
			ButtonWidget.builder(Text.literal("Cancel"), b -> openPrevScreen())
				.dimensions(width / 2 + 2, height - 48, 100, 20).build());
	}
	
	private void openFolder()
	{
		Util.getOperatingSystem().open(setting.getFolder().toFile());
	}
	
	private void openPrevScreen()
	{
		client.setScreen(prevScreen);
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
		Text title = Text.literal("Reset Folder");
		
		Text message = Text
			.literal("This will empty the '" + setting.getFolder().getFileName()
				+ "' folder and then re-generate the default files.\n"
				+ "Are you sure you want to do this?");
		
		client.setScreen(new ConfirmScreen(this::confirmReset, title, message));
	}
	
	private void confirmReset(boolean confirmed)
	{
		if(confirmed)
			setting.resetFolder();
		
		client.setScreen(SelectFileScreen.this);
	}
	
	@Override
	public boolean keyPressed(KeyInput context)
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
		doneButton.active = listGui.getSelectedOrNull() != null;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(client.textRenderer,
			setting.getName(), width / 2, 12, Colors.WHITE);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
		
		if(doneButton.isSelected() && !doneButton.active)
			context.drawTooltip(textRenderer,
				Arrays.asList(Text.literal("You must first select a file.")),
				mouseX, mouseY);
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
		extends AlwaysSelectedEntryListWidget.Entry<SelectFileScreen.Entry>
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
				"File " + path.getFileName());
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
		extends AlwaysSelectedEntryListWidget<SelectFileScreen.Entry>
	{
		public ListGui(MinecraftClient mc, SelectFileScreen screen,
			List<Path> list)
		{
			super(mc, screen.width, screen.height - 96, 36, 20);
			
			list.stream().map(SelectFileScreen.Entry::new)
				.forEach(this::addEntry);
		}
		
		public Path getSelectedPath()
		{
			SelectFileScreen.Entry selected = getSelectedOrNull();
			return selected != null ? selected.path : null;
		}
	}
}
