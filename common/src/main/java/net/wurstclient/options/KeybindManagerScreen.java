/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.wurstclient.util.gui.ManagedProfileListGui;
import net.wurstclient.util.profiles.ManagedProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.keybinds.Keybind;

public class KeybindManagerScreen extends ManagerScreen
{


	public KeybindManagerScreen(Screen prevScreen)
	{
		super(prevScreen, WurstClient.INSTANCE.getKeybinds());
	}


	@Override
	public ManagedProfileListGui getListGui() {
		return new ListGui(managedProfile, client, width, height, 36, height - 56, 30);
	}

	@Override
	public void add() {
		WurstClient.setScreen(new KeybindEditorScreen(this));
	}

	@Override
	public void edit()
	{
		Keybind keybind = WurstClient.INSTANCE.getKeybinds().getAllKeybinds()
				.get(listGui.getSelected());
		WurstClient.setScreen(new KeybindEditorScreen(this, keybind.getKey(),
				keybind.getCommands()));
	}

	@Override
	public void remove()
	{
		Keybind keybind1 = WurstClient.INSTANCE.getKeybinds().getAllKeybinds()
				.get(listGui.getSelected());
		WurstClient.INSTANCE.getKeybinds().remove(keybind1.getKey());
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
					   float partialTicks)
	{
		renderBackground(matrixStack);
		listGui.render(matrixStack, mouseX, mouseY, partialTicks);

		drawCenteredText(matrixStack, textRenderer, "Keybind Manager",
				width / 2, 8, 0xffffff);
		drawCenteredText(matrixStack, textRenderer,
				"Keybinds: " + listGui.getItemCount(), width / 2, 20, 0xffffff);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}

	private static final class ListGui extends ManagedProfileListGui
	{
		public ListGui(ManagedProfile profile, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
			super(profile, client, width, height, top, bottom, itemHeight);
		}


		@Override
		protected void renderItem(MatrixStack matrixStack, int index, int x,
								  int y, int slotHeight, int mouseX, int mouseY, float partialTicks)
		{
			Keybind keybind =
					WurstClient.INSTANCE.getKeybinds().getAllKeybinds().get(index);

			client.textRenderer.draw(matrixStack,
					"Key: " + keybind.getKey().replace("key.keyboard.", ""), x + 3,
					y + 3, 0xa0a0a0);
			client.textRenderer.draw(matrixStack,
					"Commands: " + keybind.getCommands(), x + 3, y + 15, 0xa0a0a0);
		}
	}
}