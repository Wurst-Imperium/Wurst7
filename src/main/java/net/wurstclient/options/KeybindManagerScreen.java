/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.util.ListWidget;

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
		listGui = new ListGui(client, width, height, 36, height - 56, 30);
		
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
		Keybind keybind = WurstClient.INSTANCE.getKeybinds().getAllKeybinds()
			.get(listGui.selected);
		client.setScreen(new KeybindEditorScreen(this, keybind.getKey(),
			keybind.getCommands()));
	}
	
	private void remove()
	{
		Keybind keybind1 = WurstClient.INSTANCE.getKeybinds().getAllKeybinds()
			.get(listGui.selected);
		WurstClient.INSTANCE.getKeybinds().remove(keybind1.getKey());
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);
		
		listGui.mouseClicked(mouseX, mouseY, mouseButton);
		
		if(!childClicked)
			if(mouseY >= 36 && mouseY <= height - 57)
				if(mouseX >= width / 2 + 140 || mouseX <= width / 2 - 126)
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
	public boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		listGui.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount,
			verticalAmount);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			if(editButton.active)
				editButton.onPress();
			else
				addButton.onPress();
			break;
			case GLFW.GLFW_KEY_DELETE:
			removeButton.onPress();
			break;
			case GLFW.GLFW_KEY_ESCAPE:
			backButton.onPress();
			break;
			default:
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public void tick()
	{
		boolean inBounds =
			listGui.selected > -1 && listGui.selected < listGui.getItemCount();
		
		editButton.active = inBounds;
		removeButton.active = inBounds;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context, mouseX, mouseY, partialTicks);
		listGui.render(context, mouseX, mouseY, partialTicks);
		
		context.drawCenteredTextWithShadow(textRenderer, "Keybind Manager",
			width / 2, 8, 0xffffff);
		context.drawCenteredTextWithShadow(textRenderer,
			"Keybinds: " + listGui.getItemCount(), width / 2, 20, 0xffffff);
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	private static final class ListGui extends ListWidget
	{
		private int selected = -1;
		
		public ListGui(MinecraftClient mc, int width, int height, int top,
			int bottom, int slotHeight)
		{
			super(mc, width, height, top, bottom, slotHeight);
		}
		
		@Override
		protected boolean isSelectedItem(int index)
		{
			return selected == index;
		}
		
		@Override
		protected int getItemCount()
		{
			return WurstClient.INSTANCE.getKeybinds().getAllKeybinds().size();
		}
		
		@Override
		protected boolean selectItem(int index, int int_2, double var3,
			double var4)
		{
			if(index >= 0 && index < getItemCount())
				selected = index;
			
			return true;
		}
		
		@Override
		protected void renderBackground()
		{
			
		}
		
		@Override
		protected void renderItem(DrawContext context, int index, int x, int y,
			int slotHeight, int mouseX, int mouseY, float partialTicks)
		{
			Keybind keybind =
				WurstClient.INSTANCE.getKeybinds().getAllKeybinds().get(index);
			
			context.drawText(client.textRenderer,
				"Key: " + keybind.getKey().replace("key.keyboard.", ""), x + 3,
				y + 3, 0xa0a0a0, false);
			context.drawText(client.textRenderer,
				"Commands: " + keybind.getCommands(), x + 3, y + 15, 0xa0a0a0,
				false);
		}
	}
}
