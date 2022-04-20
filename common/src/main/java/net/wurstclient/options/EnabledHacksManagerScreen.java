/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.minecraft.text.Text;
import net.wurstclient.util.gui.ManagedProfileListGui;
import net.wurstclient.util.profiles.ManagedProfile;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;

public class EnabledHacksManagerScreen extends ManagerScreen
{


	public EnabledHacksManagerScreen(Screen prevScreen)
	{
		super(prevScreen, WurstClient.INSTANCE.currentHackProfile);
	}

	@Override
	public void init(){
		super.init();
		editButton.setMessage(Text.of("Toggle"));
		editButton.x = editButton.x - editButton.getWidth()/2;
		addButton.visible = false;
		removeButton.visible = false;
	}

	@Override
	public ManagedProfileListGui getListGui() {
		return new ListGui(managedProfile, client, width, height, 36, height - 56, 30);
	}

	@Override
	public void add() {
	}

	@Override
	public void edit()
	{
		WurstClient.INSTANCE.currentHackProfile.menuToggleEnabled(listGui.getSelected());
	}

	@Override
	public void remove(){
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
					   float partialTicks)
	{
		renderBackground(matrixStack);
		listGui.render(matrixStack, mouseX, mouseY, partialTicks);

		drawCenteredText(matrixStack, textRenderer, "Enabled Hacks Manager",
				width / 2, 8, 0xffffff);
		drawCenteredText(matrixStack, textRenderer,
				"Enabled Hacks: " + listGui.getItemCount(), width / 2, 20, 0xffffff);

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
			String hackName = WurstClient.INSTANCE.currentHackProfile.getHackList().get(index);

			client.textRenderer.draw(matrixStack,
					"Name: " + hackName, x + 3,
					y + 3, 0xa0a0a0);
			client.textRenderer.draw(matrixStack,
					"Is Enabled?: " + (WurstClient.INSTANCE.currentHackProfile.isEnabled(hackName) ? "Yes": "No"), x + 3, y + 15, 0xa0a0a0);
		}

		@Override
		public int getItemCount(){
			return WurstClient.INSTANCE.currentHackProfile.size();
		}
	}
}