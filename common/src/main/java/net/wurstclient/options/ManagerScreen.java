/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.options;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.WurstClient;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.util.gui.ManagedProfileListGui;
import net.wurstclient.util.profiles.ManagedProfile;
import org.lwjgl.glfw.GLFW;

public abstract class ManagerScreen extends Screen
{
	public final Screen prevScreen;

	public ButtonWidget addButton;
	public ButtonWidget editButton;
	public ButtonWidget removeButton;
	public ButtonWidget backButton;
	public final ManagedProfile managedProfile;
	public ManagedProfileListGui listGui;
	public String managerName;

	public ManagerScreen(Screen prevScreen, ManagedProfile managedProfile)
	{
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.managedProfile = managedProfile;
	}

	@Override
	public void init()
	{
		listGui = getListGui();
		addDrawableChild(addButton = new ButtonWidget(width / 2 - 102,
				height - 52, 100, 20, new LiteralText("Add"),
				b -> add()));

		addDrawableChild(editButton = new ButtonWidget(width / 2 + 2,
				height - 52, 100, 20, new LiteralText("Edit"), b -> edit()));

		addDrawableChild(removeButton = new ButtonWidget(width / 2 - 102,
				height - 28, 100, 20, new LiteralText("Remove"), b -> remove()));

		addDrawableChild(
				backButton = new ButtonWidget(width / 2 + 2, height - 28, 100, 20,
						new LiteralText("Back"), b -> WurstClient.setScreen(prevScreen)));

		addDrawableChild(
				new ButtonWidget(8, 8, 100, 20, new LiteralText("Reset All"),
						b -> WurstClient.setScreen(new ConfirmScreen(confirmed -> {
							if(confirmed)
								managedProfile.setDefaults();
							WurstClient.setScreen(this);
						}, new LiteralText(
								"Are you sure you want to reset to Defaults?"),
								new LiteralText("This cannot be undone!")))));

		addDrawableChild(new ButtonWidget(width - 108, 8, 100, 20,
				new LiteralText("Profiles..."),
				b -> WurstClient.setScreen(new ProfilesScreen(this, managedProfile))));
	}

	public abstract ManagedProfileListGui getListGui();

	public abstract void add();

	public abstract void edit();

	public void remove()
	{
		Keybind keybind1 = WurstClient.INSTANCE.getKeybinds().getAllKeybinds()
				.get(listGui.getSelected());
		WurstClient.INSTANCE.getKeybinds().remove(keybind1.getKey());
		//managedProfile.remove(managedProfile.getList().get(listGui.getSelected()));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
	{
		boolean childClicked = super.mouseClicked(mouseX, mouseY, mouseButton);

		listGui.mouseClicked(mouseX, mouseY, mouseButton);

		if(!childClicked)
			if(mouseY >= 36 && mouseY <= height - 57)
				if(mouseX >= width / 2 + 140 || mouseX <= width / 2 - 126)
					listGui.setSelected(-1);

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
		boolean inBounds = listGui.getSelected() > -1 && listGui.getSelected() < listGui.getItemCount();

		editButton.active = inBounds;
		removeButton.active = inBounds;
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
					   float partialTicks)
	{
		renderBackground(matrixStack);
		listGui.render(matrixStack, mouseX, mouseY, partialTicks);

		drawCenteredText(matrixStack, textRenderer, managedProfile.getDisplayName() + " Manager",
				width / 2, 8, 0xffffff);
		drawCenteredText(matrixStack, textRenderer,
				"" + listGui.getItemCount(), width / 2, 20, 0xffffff);

		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}

}