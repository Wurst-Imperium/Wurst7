/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
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
		addDrawableChild(doneButton = ButtonWidget
			.builder(Text.literal(getDoneButtonText()), b -> pressDoneButton())
			.dimensions(width / 2 - 100, height / 4 + 72 + 12, 200, 20)
			.build());
		
		addDrawableChild(ButtonWidget
			.builder(Text.literal("Cancel"), b -> client.setScreen(prevScreen))
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
		nameOrEmailBox.tick();
		passwordBox.tick();
		
		String nameOrEmail = nameOrEmailBox.getText().trim();
		boolean alex = nameOrEmail.equalsIgnoreCase("Alexander01998");
		
		doneButton.active = !nameOrEmail.isEmpty()
			&& !(alex && passwordBox.getText().isEmpty());
		
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
				.resolve(URLEncoder.encode(username, "UTF-8")).toURL();
		
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
		nameOrEmailBox.mouseClicked(x, y, button);
		passwordBox.mouseClicked(x, y, button);
		
		if(nameOrEmailBox.isFocused() || passwordBox.isFocused())
			message = "";
		
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context);
		
		MatrixStack matrixStack = context.getMatrices();
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// skin preview
		AltRenderer.drawAltBack(context, nameOrEmailBox.getText(),
			(width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		AltRenderer.drawAltBody(context, nameOrEmailBox.getText(),
			width - (width / 2 - 100) / 2 - 64, height / 2 - 128, 128, 256);
		
		// text
		context.drawTextWithShadow(textRenderer, "Name (for cracked alts), or",
			width / 2 - 100, 37, 10526880);
		context.drawTextWithShadow(textRenderer, "E-Mail (for premium alts)",
			width / 2 - 100, 47, 10526880);
		context.drawTextWithShadow(textRenderer,
			"Password (leave blank for cracked alts)", width / 2 - 100, 87,
			10526880);
		
		String[] lines = message.split("\n");
		for(int i = 0; i < lines.length; i++)
			context.drawCenteredTextWithShadow(textRenderer, lines[i],
				width / 2, 142 + 10 * i, 16777215);
		
		// text boxes
		nameOrEmailBox.render(context, mouseX, mouseY, partialTicks);
		passwordBox.render(context, mouseX, mouseY, partialTicks);
		
		// red flash for errors
		if(errorTimer > 0)
		{
			GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);
			
			RenderSystem.setShaderColor(1, 0, 0, errorTimer / 16F);
			
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION);
			bufferBuilder.vertex(matrix, 0, 0, 0).next();
			bufferBuilder.vertex(matrix, width, 0, 0).next();
			bufferBuilder.vertex(matrix, width, height, 0).next();
			bufferBuilder.vertex(matrix, 0, height, 0).next();
			tessellator.draw();
			
			GL11.glEnable(GL11.GL_CULL_FACE);
			GL11.glDisable(GL11.GL_BLEND);
			errorTimer--;
		}
		
		super.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public final void close()
	{
		client.setScreen(prevScreen);
	}
}
