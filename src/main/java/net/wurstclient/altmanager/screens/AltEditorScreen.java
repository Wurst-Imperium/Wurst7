/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.SystemUtil;
import net.wurstclient.WurstClient;
import net.wurstclient.altmanager.AltRenderer;
import net.wurstclient.altmanager.NameGenerator;

public abstract class AltEditorScreen extends Screen
{
	private final Path skinFolder =
		WurstClient.INSTANCE.getWurstFolder().resolve("skins");
	
	protected final Screen prevScreen;
	
	private TextFieldWidget emailBox;
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
		addButton(
			doneButton = new ButtonWidget(width / 2 - 100, height / 4 + 72 + 12,
				200, 20, getDoneButtonText(), b -> pressDoneButton()));
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 120 + 12, 200,
			20, "Cancel", b -> minecraft.openScreen(prevScreen)));
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 96 + 12, 200,
			20, "Random Name",
			b -> emailBox.setText(NameGenerator.generateName())));
		
		addButton(stealSkinButton =
			new ButtonWidget(width - (width / 2 - 100) / 2 - 64, height - 32,
				128, 20, "Steal Skin", b -> message = stealSkin(getEmail())));
		
		addButton(new ButtonWidget((width / 2 - 100) / 2 - 64, height - 32, 128,
			20, "Open Skin Folder",
			b -> SystemUtil.getOperatingSystem().open(skinFolder.toFile())));
		
		emailBox = new TextFieldWidget(font, width / 2 - 100, 60, 200, 20, "");
		emailBox.setMaxLength(48);
		emailBox.method_1876(true);
		emailBox.setText(getDefaultEmail());
		children.add(emailBox);
		
		passwordBox =
			new TextFieldWidget(font, width / 2 - 100, 100, 200, 20, "");
		passwordBox.setText(getDefaultPassword());
		passwordBox.setRenderTextProvider((text, int_1) -> {
			String stars = "";
			for(int i = 0; i < text.length(); i++)
				stars += "*";
			return stars;
		});
		children.add(passwordBox);
		
		setInitialFocus(emailBox);
	}
	
	@Override
	public final void tick()
	{
		emailBox.tick();
		passwordBox.tick();
		
		String email = emailBox.getText().trim();
		boolean alex = email.equalsIgnoreCase("Alexander01998");
		
		doneButton.active =
			!email.isEmpty() && !(alex && passwordBox.getText().isEmpty());
		
		stealSkinButton.active = !alex;
	}
	
	protected final String getEmail()
	{
		return emailBox.getText();
	}
	
	protected final String getPassword()
	{
		return passwordBox.getText();
	}
	
	protected String getDefaultEmail()
	{
		return minecraft.getSession().getUsername();
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
		String skin = name + ".png";
		
		URI u = URI.create("http://skins.minecraft.net/MinecraftSkins/")
			.resolve(skin);
		Path path = skinFolder.resolve(skin);
		
		try(InputStream in = u.toURL().openStream())
		{
			Files.copy(in, path);
			return "§a§lSaved skin as " + skin;
			
		}catch(IOException e)
		{
			e.printStackTrace();
			return "§4§lSkin could not be saved.";
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
		emailBox.mouseClicked(x, y, button);
		passwordBox.mouseClicked(x, y, button);
		
		if(emailBox.isFocused() || passwordBox.isFocused())
			message = "";
		
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public void render(int mouseX, int mouseY, float partialTicks)
	{
		renderBackground();
		
		// skin preview
		AltRenderer.drawAltBack(emailBox.getText(), (width / 2 - 100) / 2 - 64,
			height / 2 - 128, 128, 256);
		AltRenderer.drawAltBody(emailBox.getText(),
			width - (width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		
		// text
		drawString(font, "Name or E-Mail", width / 2 - 100, 47, 10526880);
		drawString(font, "Password", width / 2 - 100, 87, 10526880);
		drawCenteredString(font, message, width / 2, 142, 16777215);
		
		// text boxes
		emailBox.render(mouseX, mouseY, partialTicks);
		passwordBox.render(mouseX, mouseY, partialTicks);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			
			GL11.glColor4f(1, 0, 0, errorTimer / 16F);
			
			GL11.glBegin(GL11.GL_QUADS);
			{
				GL11.glVertex2d(0, 0);
				GL11.glVertex2d(width, 0);
				GL11.glVertex2d(width, height);
				GL11.glVertex2d(0, height);
			}
			GL11.glEnd();
			
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			errorTimer--;
		}
		
		super.render(mouseX, mouseY, partialTicks);
	}
}
