/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.wurstclient.WurstClient;
import net.wurstclient.util.ListWidget;
import net.wurstclient.util.json.JsonException;

public final class KeybindProfilesScreen extends Screen
{
	private final Screen prevScreen;
	
	private ListGui listGui;
	private ButtonWidget loadButton;
	
	public KeybindProfilesScreen(Screen prevScreen)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
	}
	
	@Override
	public void init()
	{
		listGui = new ListGui(client, this,
			WurstClient.INSTANCE.getKeybinds().listProfiles());
		
		addButton(new ButtonWidget(8, 8, 100, 20,
			new LiteralText("Open Folder"), b -> openFolder()));
		
		addButton(new ButtonWidget(width / 2 - 154, height - 48, 100, 20,
			new LiteralText("New Profile"), b -> client.openScreen(
				new EnterProfileNameScreen(this, this::newProfile))));
		
		loadButton = addButton(new ButtonWidget(width / 2 - 50, height - 48,
			100, 20, new LiteralText("Load"), b -> loadSelected()));
		
		addButton(new ButtonWidget(width / 2 + 54, height - 48, 100, 20,
			new LiteralText("Cancel"), b -> openPrevScreen()));
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
		if(listGui.selected < 0 || listGui.selected >= listGui.list.size())
		{
			openPrevScreen();
			return;
		}
		
		Path path = listGui.list.get(listGui.selected);
		String fileName = "" + path.getFileName();
		
		try
		{
			WurstClient.INSTANCE.getKeybinds().loadProfile(fileName);
			openPrevScreen();
			
		}catch(IOException | JsonException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	private void openPrevScreen()
	{
		client.openScreen(prevScreen);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked && (mouseX < (width - 220) / 2
			|| mouseX > width / 2 + 129 || mouseY < 32 || mouseY > height - 64))
			listGui.selected = -1;
		
		return childClicked;
	}
	
	@Override
	public boolean mouseDragged(double double_1, double double_2, int int_1,
		double double_3, double double_4)
	{
		listGui.mouseDragged(double_1, double_2, int_1, double_3, double_4);
		return super.mouseDragged(double_1, double_2, int_1, double_3,
			double_4);
	}
	
	@Override
	public boolean mouseReleased(double double_1, double double_2, int int_1)
	{
		listGui.mouseReleased(double_1, double_2, int_1);
		return super.mouseReleased(double_1, double_2, int_1);
	}
	
	@Override
	public boolean mouseScrolled(double double_1, double double_2,
		double double_3)
	{
		listGui.mouseScrolled(double_1, double_2, double_3);
		return super.mouseScrolled(double_1, double_2, double_3);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			loadSelected();
		else if(keyCode == GLFW.GLFW_KEY_ESCAPE)
			openPrevScreen();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		loadButton.active =
			listGui.selected >= 0 && listGui.selected < listGui.list.size();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		listGui.render(matrixStack, mouseX, mouseY, partialTicks);
		
		drawCenteredString(matrixStack, client.textRenderer, "Keybind Profiles",
			width / 2, 12, 0xffffff);
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		
		if(loadButton.isHovered() && !loadButton.active)
			renderTooltip(matrixStack,
				Arrays.asList(new LiteralText("You must first select a file.")),
				mouseX, mouseY);
	}
	
	private static class ListGui extends ListWidget
	{
		private final MinecraftClient mc;
		private final List<Path> list;
		private int selected = -1;
		
		public ListGui(MinecraftClient mc, KeybindProfilesScreen screen,
			ArrayList<Path> list)
		{
			super(mc, screen.width, screen.height, 36, screen.height - 64, 20);
			this.mc = mc;
			this.list = list;
		}
		
		@Override
		protected int getItemCount()
		{
			return list.size();
		}
		
		@Override
		protected boolean selectItem(int index, int int_2, double var3,
			double var4)
		{
			if(index >= 0 && index < list.size())
				selected = index;
			
			return true;
		}
		
		@Override
		protected boolean isSelectedItem(int index)
		{
			return index == selected;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(MatrixStack matrixStack, int index, int x,
			int y, int var4, int var5, int var6, float partialTicks)
		{
			TextRenderer fr = mc.textRenderer;
			
			Path path = list.get(index);
			fr.draw(matrixStack, "" + path.getFileName(), x + 28, y, 0xf0f0f0);
			fr.draw(matrixStack,
				"" + client.runDirectory.toPath().relativize(path), x + 28,
				y + 9, 0xa0a0a0);
		}
	}
}
