/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.TemplateToolState;

public final class ChooseNameState extends TemplateToolState
{
	@Override
	public void onEnter(TemplateToolHack hack)
	{
		MC.setScreen(new ChooseNameScreen(hack));
	}
	
	@Override
	public void onExit(TemplateToolHack hack)
	{
		MC.setScreen(null);
	}
	
	@Override
	protected String getMessage(TemplateToolHack hack)
	{
		File file = hack.getFile();
		if(file != null && file.exists())
			return "WARNING: This file already exists.";
		
		return "Choose a name for this template.";
	}
	
	public static final class ChooseNameScreen extends Screen
	{
		private static final WurstClient WURST = WurstClient.INSTANCE;
		private final TemplateToolHack hack;
		
		private EditBox nameField;
		private Checkbox includeTypesBox;
		private Button doneButton;
		private Button cancelButton;
		
		public ChooseNameScreen(TemplateToolHack hack)
		{
			super(CommonComponents.EMPTY);
			this.hack = hack;
		}
		
		@Override
		public void init()
		{
			Font tr = minecraft.font;
			int middleX = width / 2;
			int middleY = height / 2;
			
			nameField = new EditBox(tr, middleX - 99, middleY + 16, 198, 16,
				Component.empty());
			nameField.setBordered(false);
			nameField.setMaxLength(32);
			nameField.setFocused(true);
			nameField.setTextColor(CommonColors.WHITE);
			addWidget(nameField);
			setFocused(nameField);
			
			includeTypesBox =
				Checkbox.builder(Component.literal("Include block types"), tr)
					.pos(middleX - 99, middleY + 32).selected(true).build();
			addRenderableWidget(includeTypesBox);
			
			doneButton = Button.builder(Component.literal("Done"), b -> done())
				.bounds(middleX - 75, middleY + 56, 150, 20).build();
			addRenderableWidget(doneButton);
			
			cancelButton =
				Button.builder(Component.literal("Cancel"), b -> cancel())
					.bounds(middleX - 50, middleY + 80, 100, 15).build();
			addRenderableWidget(cancelButton);
		}
		
		private void done()
		{
			if(hack.getFile() == null)
				return;
			
			hack.setBlockTypesEnabled(includeTypesBox.selected());
			hack.setState(new SavingFileState());
		}
		
		private void cancel()
		{
			hack.setEnabled(false);
		}
		
		@Override
		public void tick()
		{
			if(nameField.getValue().isEmpty())
				hack.setFile(null);
			else
				try
				{
					Path folder = WURST.getHax().autoBuildHack.getFolder();
					Path file = folder.resolve(nameField.getValue() + ".json");
					hack.setFile(file.toFile());
					
				}catch(InvalidPathException e)
				{
					hack.setFile(null);
				}
			
			doneButton.active = hack.getFile() != null;
		}
		
		@Override
		public boolean keyPressed(KeyEvent context)
		{
			switch(context.key())
			{
				case GLFW.GLFW_KEY_ESCAPE:
				cancelButton.onPress(context);
				break;
				
				case GLFW.GLFW_KEY_ENTER:
				doneButton.onPress(context);
				break;
			}
			
			return super.keyPressed(context);
		}
		
		@Override
		public void render(GuiGraphics context, int mouseX, int mouseY,
			float partialTicks)
		{
			super.render(context, mouseX, mouseY, partialTicks);
			
			// middle
			int middleX = width / 2;
			int middleY = height / 2;
			
			// background positions
			int x1 = middleX - 100;
			int y1 = middleY + 15;
			int x2 = middleX + 100;
			int y2 = middleY + 26;
			
			// background
			context.fill(x1, y1, x2, y2, 0x80000000);
			
			// name field
			nameField.render(context, mouseX, mouseY, partialTicks);
		}
		
		@Override
		public void renderBackground(GuiGraphics context, int mouseX,
			int mouseY, float deltaTicks)
		{
			// Don't blur
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
	}
}
