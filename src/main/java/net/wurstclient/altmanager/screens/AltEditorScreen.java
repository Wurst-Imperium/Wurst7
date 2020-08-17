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
		passwordBox.setMaxLength(256);
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
			URL url = getSkinUrl(name);
			
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
	
	/**
	 * Returns the skin download URL for the given username.
	 */
	public URL getSkinUrl(String username) throws IOException
	{
		String uuid = getUUID(username);
		JsonObject texturesValueJson = getTexturesValue(uuid);
		
		// Grab URL for skin
		JsonObject tJObj = texturesValueJson.get("textures").getAsJsonObject();
		JsonObject skinJObj = tJObj.get("SKIN").getAsJsonObject();
		String skin = skinJObj.get("url").getAsString();
		
		return URI.create(skin).toURL();
	}
	
	/**
	 * Decodes the base64 textures value from {@link #getSessionJson(String)}.
	 * Once decoded, it looks like this:
	 *
	 * <code><pre>
	 * {
	 *   "timestamp" : &lt;current time&gt;,
	 *   "profileId" : "&lt;UUID&gt;",
	 *   "profileName" : "&lt;username&gt;",
	 *   "textures":
	 *   {
	 *     "SKIN":
	 *     {
	 *       "url": "http://textures.minecraft.net/texture/&lt;texture ID&gt;"
	 *     }
	 *   }
	 * }
	 * </pre></code>
	 */
	private JsonObject getTexturesValue(String uuid) throws IOException
	{
		JsonObject sessionJson = getSessionJson(uuid);
		
		JsonArray propertiesJson =
			sessionJson.get("properties").getAsJsonArray();
		JsonObject firstProperty = propertiesJson.get(0).getAsJsonObject();
		String texturesBase64 = firstProperty.get("value").getAsString();
		
		byte[] texturesBytes = Base64.decodeBase64(texturesBase64.getBytes());
		JsonObject texturesJson =
			new Gson().fromJson(new String(texturesBytes), JsonObject.class);
		
		return texturesJson;
	}
	
	/**
	 * Grabs the JSON code from the session server. It looks something like
	 * this:
	 *
	 * <code><pre>
	 * {
	 *   "id": "&lt;UUID&gt;",
	 *   "name": "&lt;username&gt;",
	 *   "properties":
	 *   [
	 *     {
	 *       "name": "textures",
	 *       "value": "&lt;base64 encoded JSON&gt;"
	 *     }
	 *   ]
	 * }
	 * </pre></code>
	 */
	private JsonObject getSessionJson(String uuid) throws IOException
	{
		URL sessionURL = URI
			.create(
				"https://sessionserver.mojang.com/session/minecraft/profile/")
			.resolve(uuid).toURL();
		
		try(InputStream sessionInputStream = sessionURL.openStream())
		{
			return new Gson().fromJson(
				IOUtils.toString(sessionInputStream, StandardCharsets.UTF_8),
				JsonObject.class);
		}
	}
	
	private String getUUID(String username) throws IOException
	{
		URL profileURL =
			URI.create("https://api.mojang.com/users/profiles/minecraft/")
				.resolve(username).toURL();
		
		try(InputStream profileInputStream = profileURL.openStream())
		{
			// {"name":"<username>","id":"<UUID>"}
			
			JsonObject profileJson = new Gson().fromJson(
				IOUtils.toString(profileInputStream, StandardCharsets.UTF_8),
				JsonObject.class);
			
			return profileJson.get("id").getAsString();
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
		
		// skin preview
		AltRenderer.drawAltBack(matrixStack, emailBox.getText(),
			(width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		AltRenderer.drawAltBody(matrixStack, emailBox.getText(),
			width - (width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		
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
