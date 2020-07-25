/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import net.wurstclient.util.SkinUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
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
		addButton(doneButton =
			new ButtonWidget(width / 2 - 100, height / 4 + 72 + 12, 200, 20,
				new LiteralText(getDoneButtonText()), b -> pressDoneButton()));
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 120 + 12, 200,
			20, new LiteralText("Cancel"), b -> client.openScreen(prevScreen)));
		
		addButton(new ButtonWidget(width / 2 - 100, height / 4 + 96 + 12, 200,
			20, new LiteralText("Random Name"),
			b -> emailBox.setText(NameGenerator.generateName())));
		
		addButton(stealSkinButton =
			new ButtonWidget(width - (width / 2 - 100) / 2 - 64, height - 32,
				128, 20, new LiteralText("Steal Skin"),
				b -> message = stealSkin(getEmail())));
		
		addButton(new ButtonWidget((width / 2 - 100) / 2 - 64, height - 32, 128,
			20, new LiteralText("Open Skin Folder"), b -> openSkinFolder()));
		
		emailBox = new TextFieldWidget(textRenderer, width / 2 - 100, 60, 200,
			20, new LiteralText(""));
		emailBox.setMaxLength(48);
		emailBox.setSelected(true);
		emailBox.setText(getDefaultEmail());
		children.add(emailBox);
		
		passwordBox = new TextFieldWidget(textRenderer, width / 2 - 100, 100,
			200, 20, new LiteralText(""));
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
		emailBox.tick();
		passwordBox.tick();
		
		String email = emailBox.getText().trim();
		
		doneButton.active =	!email.isEmpty();
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
			URL url = SkinUtils.getSkinUrl(name);
			
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
		emailBox.mouseClicked(x, y, button);
		passwordBox.mouseClicked(x, y, button);
		
		if(emailBox.isFocused() || passwordBox.isFocused())
			message = "";
		
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(matrixStack);
		
		// skin preview, disabled for Mojang Accounts
		if(!emailBox.getText().contains("@")){
			AltRenderer.drawAltBack(matrixStack, emailBox.getText(),
					(width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
			AltRenderer.drawAltBody(matrixStack, emailBox.getText(),
					width - (width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		}

		
		// text
		drawStringWithShadow(matrixStack, textRenderer, "Name or E-Mail",
			width / 2 - 100, 47, 10526880);
		drawStringWithShadow(matrixStack, textRenderer, "Password",
			width / 2 - 100, 87, 10526880);
		drawCenteredString(matrixStack, textRenderer, message, width / 2, 142,
			16777215);
		
		// text boxes
		emailBox.render(matrixStack, mouseX, mouseY, partialTicks);
		passwordBox.render(matrixStack, mouseX, mouseY, partialTicks);
		
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
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
}
