/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import java.io.File;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
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
		
		private TextFieldWidget nameField;
		private CheckboxWidget includeTypesBox;
		private ButtonWidget doneButton;
		private ButtonWidget cancelButton;
		
		public ChooseNameScreen(TemplateToolHack hack)
		{
			super(ScreenTexts.EMPTY);
			this.hack = hack;
		}
		
		@Override
		public void init()
		{
			TextRenderer tr = client.textRenderer;
			int middleX = width / 2;
			int middleY = height / 2;
			
			nameField = new TextFieldWidget(tr, middleX - 99, middleY + 16, 198,
				16, Text.empty());
			nameField.setDrawsBackground(false);
			nameField.setMaxLength(32);
			nameField.setFocused(true);
			nameField.setEditableColor(Colors.WHITE);
			addSelectableChild(nameField);
			setFocused(nameField);
			
			includeTypesBox =
				CheckboxWidget.builder(Text.literal("Include block types"), tr)
					.pos(middleX - 99, middleY + 32).checked(true).build();
			addDrawableChild(includeTypesBox);
			
			doneButton = ButtonWidget.builder(Text.literal("Done"), b -> done())
				.dimensions(middleX - 75, middleY + 56, 150, 20).build();
			addDrawableChild(doneButton);
			
			cancelButton =
				ButtonWidget.builder(Text.literal("Cancel"), b -> cancel())
					.dimensions(middleX - 50, middleY + 80, 100, 15).build();
			addDrawableChild(cancelButton);
		}
		
		private void done()
		{
			hack.setBlockTypesEnabled(includeTypesBox.isChecked());
			hack.setState(new SavingFileState());
		}
		
		private void cancel()
		{
			hack.setEnabled(false);
		}
		
		@Override
		public void tick()
		{
			if(nameField.getText().isEmpty())
				return;
			
			Path folder = WURST.getHax().autoBuildHack.getFolder();
			Path file = folder.resolve(nameField.getText() + ".json");
			hack.setFile(file.toFile());
		}
		
		@Override
		public boolean keyPressed(KeyInput context)
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
		public void render(DrawContext context, int mouseX, int mouseY,
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
		public void renderBackground(DrawContext context, int mouseX,
			int mouseY, float deltaTicks)
		{
			// Don't blur
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
	}
}
