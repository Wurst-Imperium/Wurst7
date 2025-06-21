/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Util;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.AltRenderer;
import net.wurstclient.altmanager.NameGenerator;
import net.wurstclient.altmanager.SkinStealer;

public abstract class AltEditorScreen extends Screen
{
	private final Path skinFolder =
		WurstClient.INSTANCE.getWurstFolder().resolve("skins");
	
	protected final Screen prevScreen;
	
	private TextFieldWidget nameOrEmailBox;
	private TextFieldWidget passwordBox;
	
	private ButtonWidget doneButton;
	private ButtonWidget stealSkinButton;
	
	protected String message = "";
	private int errorTimer;
	
	public AltEditorScreen(Screen prevScreen, Text title)
	{
		super(title);
		this.prevScreen = prevScreen;
	}
	
	@Override
	public final void init()
	{
		nameOrEmailBox = new TextFieldWidget(textRenderer, width / 2 - 100, 60,
			200, 20, Text.literal(""));
		nameOrEmailBox.setMaxLength(48);
		nameOrEmailBox.setFocused(true);
		nameOrEmailBox.setText(getDefaultNameOrEmail());
		addSelectableChild(nameOrEmailBox);
		
		passwordBox = new TextFieldWidget(textRenderer, width / 2 - 100, 100,
			200, 20, Text.literal(""));
		passwordBox.setText(getDefaultPassword());
		passwordBox.setRenderTextProvider((text, int_1) -> {
			String stars = "";
			for(int i = 0; i < text.length(); i++)
				stars += "*";
			return OrderedText.styledForwardsVisitedString(stars, Style.EMPTY);
		});
		passwordBox.setMaxLength(256);
		addSelectableChild(passwordBox);
		
		addDrawableChild(doneButton = ButtonWidget
			.builder(Text.literal(getDoneButtonText()), b -> pressDoneButton())
			.dimensions(width / 2 - 100, height / 4 + 72 + 12, 200, 20)
			.build());
		
		addDrawableChild(
			ButtonWidget.builder(Text.literal("Cancel"), b -> close())
				.dimensions(width / 2 - 100, height / 4 + 120 + 12, 200, 20)
				.build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Random Name"),
				b -> nameOrEmailBox.setText(NameGenerator.generateName()))
			.dimensions(width / 2 - 100, height / 4 + 96 + 12, 200, 20)
			.build());
		
		addDrawableChild(stealSkinButton = ButtonWidget
			.builder(Text.literal("Steal Skin"),
				b -> message = stealSkin(getNameOrEmail()))
			.dimensions(width - (width / 2 - 100) / 2 - 64, height - 32, 128,
				20)
			.build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Open Skin Folder"), b -> openSkinFolder())
			.dimensions((width / 2 - 100) / 2 - 64, height - 32, 128, 20)
			.build());
		
		setFocused(nameOrEmailBox);
	}
	
	private void openSkinFolder()
	{
		createSkinFolder();
		Util.getOperatingSystem().open(skinFolder.toFile());
	}
	
	private void createSkinFolder()
	{
		try
		{
			Files.createDirectories(skinFolder);
			
		}catch(IOException e)
		{
			e.printStackTrace();
			message = "\u00a74\u00a7lSkin folder could not be created.";
		}
	}
	
	@Override
	public final void tick()
	{
		String nameOrEmail = nameOrEmailBox.getText().trim();
		boolean alex = nameOrEmail.equalsIgnoreCase("Alexander01998");
		
		doneButton.active = !nameOrEmail.isEmpty()
			&& !(alex && passwordBox.getText().isEmpty());
		doneButton.setMessage(Text.literal(getDoneButtonText()));
		
		stealSkinButton.active = !alex;
	}
	
	/**
	 * @return the user-entered name or email. Cannot be empty when pressing the
	 *         done button. Cannot be null.
	 */
	protected final String getNameOrEmail()
	{
		return nameOrEmailBox.getText();
	}
	
	/**
	 * @return the user-entered password. Can be empty. Cannot be null.
	 */
	protected final String getPassword()
	{
		return passwordBox.getText();
	}
	
	protected String getDefaultNameOrEmail()
	{
		return client.getSession().getUsername();
	}
	
	protected String getDefaultPassword()
	{
		return "";
	}
	
	protected abstract String getDoneButtonText();
	
	protected abstract void pressDoneButton();
	
	protected final void doErrorEffect()
	{
		errorTimer = 8;
	}
	
	private final String stealSkin(String name)
	{
		createSkinFolder();
		Path path = skinFolder.resolve(name + ".png");
		
		try
		{
			URL url = SkinStealer.getSkinUrl(name);
			
			try(InputStream in = url.openStream())
			{
				Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			}
			
			return "\u00a7a\u00a7lSaved skin as " + name + ".png";
			
		}catch(IOException e)
		{
			e.printStackTrace();
			return "\u00a74\u00a7lSkin could not be saved.";
			
		}catch(NullPointerException e)
		{
			e.printStackTrace();
			return "\u00a74\u00a7lPlayer does not exist.";
		}
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			doneButton.onPress();
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int button)
	{
		nameOrEmailBox.mouseClicked(x, y, button);
		passwordBox.mouseClicked(x, y, button);
		
		if(nameOrEmailBox.isFocused() || passwordBox.isFocused())
			message = "";
		
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			close();
			return true;
		}
		
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		// skin preview
		AltRenderer.drawAltBack(context, nameOrEmailBox.getText(),
			(width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		AltRenderer.drawAltBody(context, nameOrEmailBox.getText(),
			width - (width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		
		String accountType = getPassword().isEmpty() ? "cracked" : "premium";
		
		// text
		context.drawTextWithShadow(textRenderer, "Name (for cracked alts), or",
			width / 2 - 100, 37, Colors.LIGHT_GRAY);
		context.drawTextWithShadow(textRenderer, "E-Mail (for premium alts)",
			width / 2 - 100, 47, Colors.LIGHT_GRAY);
		context.drawTextWithShadow(textRenderer, "Password (for premium alts)",
			width / 2 - 100, 87, Colors.LIGHT_GRAY);
		context.drawTextWithShadow(textRenderer, "Account type: " + accountType,
			width / 2 - 100, 127, Colors.LIGHT_GRAY);
		
		String[] lines = message.split("\n");
		for(int i = 0; i < lines.length; i++)
			context.drawCenteredTextWithShadow(textRenderer, lines[i],
				width / 2, 142 + 10 * i, Colors.WHITE);
		
		// text boxes
		nameOrEmailBox.render(context, mouseX, mouseY, partialTicks);
		passwordBox.render(context, mouseX, mouseY, partialTicks);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			int alpha = (int)(Math.min(1, errorTimer / 16F) * 255);
			int color = 0xFF0000 | alpha << 24;
			context.fill(0, 0, width, height, color);
			errorTimer--;
		}
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public final void close()
	{
		client.setScreen(prevScreen);
	}
}
