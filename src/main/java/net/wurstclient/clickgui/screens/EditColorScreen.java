/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.ColorUtils;
import net.wurstclient.util.WurstColors;

public final class EditColorScreen extends Screen
{
	private final Screen prevScreen;
	private final ColorSetting colorSetting;
	private Color color;
	
	private EditBox hexValueField;
	private EditBox redValueField;
	private EditBox greenValueField;
	private EditBox blueValueField;
	
	private Button doneButton;
	
	private final ResourceLocation paletteIdentifier =
		ResourceLocation.fromNamespaceAndPath("wurst", "colorpalette.png");
	private BufferedImage paletteAsBufferedImage;
	
	private int paletteX = 0;
	private int paletteY = 0;
	
	private final int paletteWidth = 200;
	private final int paletteHeight = 84;
	
	private int fieldsX = 0;
	private int fieldsY = 0;
	
	private boolean ignoreChanges;
	
	public EditColorScreen(Screen prevScreen, ColorSetting colorSetting)
	{
		super(Component.literal(""));
		this.prevScreen = prevScreen;
		this.colorSetting = colorSetting;
		color = colorSetting.getColor();
	}
	
	@Override
	public void init()
	{
		// Cache color palette
		try(InputStream stream = minecraft.getResourceManager()
			.getResourceOrThrow(paletteIdentifier).open())
		{
			paletteAsBufferedImage = ImageIO.read(stream);
			
		}catch(IOException e)
		{
			paletteAsBufferedImage = null;
			e.printStackTrace();
		}
		
		Font tr = minecraft.font;
		paletteX = width / 2 - 100;
		paletteY = 32;
		fieldsX = width / 2 - 100;
		fieldsY = 129 + 5;
		
		hexValueField =
			new EditBox(tr, fieldsX, fieldsY, 92, 20, Component.literal(""));
		hexValueField.setValue(ColorUtils.toHex(color).substring(1));
		hexValueField.setMaxLength(6);
		hexValueField.setResponder(s -> updateColor(true));
		
		// RGB fields
		redValueField = new EditBox(tr, fieldsX, fieldsY + 35, 50, 20,
			Component.literal(""));
		redValueField.setValue("" + color.getRed());
		redValueField.setMaxLength(3);
		redValueField.setResponder(s -> updateColor(false));
		
		greenValueField = new EditBox(tr, fieldsX + 75, fieldsY + 35, 50, 20,
			Component.literal(""));
		greenValueField.setValue("" + color.getGreen());
		greenValueField.setMaxLength(3);
		greenValueField.setResponder(s -> updateColor(false));
		
		blueValueField = new EditBox(tr, fieldsX + 150, fieldsY + 35, 50, 20,
			Component.literal(""));
		blueValueField.setValue("" + color.getBlue());
		blueValueField.setMaxLength(3);
		blueValueField.setResponder(s -> updateColor(false));
		
		addWidget(hexValueField);
		addWidget(redValueField);
		addWidget(greenValueField);
		addWidget(blueValueField);
		
		setFocused(hexValueField);
		hexValueField.setFocused(true);
		hexValueField.setCursorPosition(0);
		hexValueField.setHighlightPos(6);
		
		doneButton = Button.builder(Component.literal("Done"), b -> done())
			.bounds(fieldsX, height - 30, 200, 20).build();
		addRenderableWidget(doneButton);
	}
	
	private void updateColor(boolean hex)
	{
		if(ignoreChanges)
			return;
		
		Color newColor;
		
		if(hex)
			newColor = ColorUtils.tryParseHex("#" + hexValueField.getValue());
		else
			newColor = ColorUtils.tryParseRGB(redValueField.getValue(),
				greenValueField.getValue(), blueValueField.getValue());
		
		if(newColor == null || newColor.equals(color))
			return;
		
		color = newColor;
		ignoreChanges = true;
		hexValueField.setValue(ColorUtils.toHex(color).substring(1));
		redValueField.setValue("" + color.getRed());
		greenValueField.setValue("" + color.getGreen());
		blueValueField.setValue("" + color.getBlue());
		ignoreChanges = false;
	}
	
	private void done()
	{
		colorSetting.setColor(color);
		minecraft.setScreen(prevScreen);
	}
	
	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY,
		float partialTicks)
	{
		Font tr = minecraft.font;
		
		context.drawCenteredString(minecraft.font, colorSetting.getName(),
			width / 2, 16, WurstColors.VERY_LIGHT_GRAY);
		
		// Draw palette
		int x = paletteX;
		int y = paletteY;
		int w = paletteWidth;
		int h = paletteHeight;
		int fw = paletteWidth;
		int fh = paletteHeight;
		float u = 0;
		float v = 0;
		context.blit(RenderPipelines.GUI_TEXTURED, paletteIdentifier, x, y, u,
			v, w, h, fw, fh);
		
		// RGB letters
		context.drawString(tr, "#", fieldsX - 3 - tr.width("#"), fieldsY + 6,
			WurstColors.VERY_LIGHT_GRAY, false);
		context.drawString(tr, "R:", fieldsX - 3 - tr.width("R:"),
			fieldsY + 6 + 35, CommonColors.RED, false);
		context.drawString(tr, "G:", fieldsX + 75 - 3 - tr.width("G:"),
			fieldsY + 6 + 35, CommonColors.GREEN, false);
		context.drawString(tr, "B:", fieldsX + 150 - 3 - tr.width("B:"),
			fieldsY + 6 + 35, CommonColors.BLUE, false);
		
		hexValueField.render(context, mouseX, mouseY, partialTicks);
		redValueField.render(context, mouseX, mouseY, partialTicks);
		greenValueField.render(context, mouseX, mouseY, partialTicks);
		blueValueField.render(context, mouseX, mouseY, partialTicks);
		
		// Color preview
		
		int borderSize = 1;
		int boxWidth = 92;
		int boxHeight = 20;
		int boxX = width / 2 + 8;
		int boxY = fieldsY;
		
		// Border
		context.fill(boxX - borderSize, boxY - borderSize,
			boxX + boxWidth + borderSize, boxY + boxHeight + borderSize,
			CommonColors.LIGHT_GRAY);
		
		// Color box
		context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight,
			color.getRGB());
		
		for(Renderable drawable : renderables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void resize(Minecraft client, int width, int height)
	{
		String hex = hexValueField.getValue();
		String r = redValueField.getValue();
		String g = greenValueField.getValue();
		String b = blueValueField.getValue();
		
		init(client, width, height);
		
		hexValueField.setValue(hex);
		redValueField.setValue(r);
		greenValueField.setValue(g);
		blueValueField.setValue(b);
	}
	
	@Override
	public boolean keyPressed(KeyEvent context)
	{
		switch(context.key())
		{
			case GLFW.GLFW_KEY_ENTER:
			done();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
			minecraft.setScreen(prevScreen);
			break;
		}
		
		return super.keyPressed(context);
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent context, boolean doubleClick)
	{
		double mouseX = context.x();
		double mouseY = context.y();
		
		if(mouseX >= paletteX && mouseX <= paletteX + paletteWidth
			&& mouseY >= paletteY && mouseY <= paletteY + paletteHeight)
		{
			if(paletteAsBufferedImage == null)
				return super.mouseClicked(context, doubleClick);
			
			int x = (int)Math.round((mouseX - paletteX) / paletteWidth
				* paletteAsBufferedImage.getWidth());
			int y = (int)Math.round((mouseY - paletteY) / paletteHeight
				* paletteAsBufferedImage.getHeight());
			
			if(x > 0 && y > 0 && x < paletteAsBufferedImage.getWidth()
				&& y < paletteAsBufferedImage.getHeight())
			{
				int rgb = paletteAsBufferedImage.getRGB(x, y);
				Color color = new Color(rgb, true);
				
				// Set color if pixel has full alpha
				if(color.getAlpha() >= 255)
					setColor(color);
			}
		}
		
		return super.mouseClicked(context, doubleClick);
	}
	
	private void setColor(Color color)
	{
		hexValueField.setValue(ColorUtils.toHex(color).substring(1));
		redValueField.setValue("" + color.getRed());
		greenValueField.setValue("" + color.getGreen());
		blueValueField.setValue("" + color.getBlue());
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
