/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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

import net.wurstclient.WurstClient;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.ColorUtils;
import net.wurstclient.core.MCScreen;

public final class EditColorScreen extends MCScreen
{
	private final Screen prevScreen;
	private final ColorSetting colorSetting;
	private Color color;
	
	private TextFieldWidget hexValueField;
	private TextFieldWidget redValueField;
	private TextFieldWidget greenValueField;
	private TextFieldWidget blueValueField;
	
	private ButtonWidget doneButton;
	
	private final Identifier paletteIdentifier =
		new Identifier("wurst", "colorpalette.png");
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
		super(new LiteralText(""));
		this.prevScreen = prevScreen;
		this.colorSetting = colorSetting;
		color = colorSetting.getColor();
	}
	
	@Override
	public void init()
	{
		// Cache color palette
		try(InputStream stream = client.getResourceManager()
			.getResource(paletteIdentifier).getInputStream())
		{
			paletteAsBufferedImage = ImageIO.read(stream);
			
		}catch(IOException e)
		{
			paletteAsBufferedImage = null;
			e.printStackTrace();
		}
		
		TextRenderer tr = client.textRenderer;
		paletteX = width / 2 - 100;
		paletteY = 32;
		fieldsX = width / 2 - 100;
		fieldsY = 129 + 5;
		
		hexValueField = new TextFieldWidget(tr, fieldsX, fieldsY, 92, 20,
			new LiteralText(""));
		hexValueField.setText(ColorUtils.toHex(color).substring(1));
		hexValueField.setMaxLength(6);
		hexValueField.setChangedListener(s -> updateColor(true));
		
		// RGB fields
		redValueField = new TextFieldWidget(tr, fieldsX, fieldsY + 35, 50, 20,
			new LiteralText(""));
		redValueField.setText("" + color.getRed());
		redValueField.setMaxLength(3);
		redValueField.setChangedListener(s -> updateColor(false));
		
		greenValueField = new TextFieldWidget(tr, fieldsX + 75, fieldsY + 35,
			50, 20, new LiteralText(""));
		greenValueField.setText("" + color.getGreen());
		greenValueField.setMaxLength(3);
		greenValueField.setChangedListener(s -> updateColor(false));
		
		blueValueField = new TextFieldWidget(tr, fieldsX + 150, fieldsY + 35,
			50, 20, new LiteralText(""));
		blueValueField.setText("" + color.getBlue());
		blueValueField.setMaxLength(3);
		blueValueField.setChangedListener(s -> updateColor(false));
		
		addSelectableChild(hexValueField);
		addSelectableChild(redValueField);
		addSelectableChild(greenValueField);
		addSelectableChild(blueValueField);
		
		setInitialFocus(hexValueField);
		hexValueField.setTextFieldFocused(true);
		hexValueField.setSelectionStart(0);
		hexValueField.setSelectionEnd(6);
		
		doneButton = new ButtonWidget(fieldsX, height - 30, 200, 20,
			new LiteralText("Done"), b -> done());
		addDrawableChild(doneButton);
	}
	
	private void updateColor(boolean hex)
	{
		if(ignoreChanges)
			return;
		
		Color newColor;
		
		if(hex)
			newColor = ColorUtils.tryParseHex("#" + hexValueField.getText());
		else
			newColor = ColorUtils.tryParseRGB(redValueField.getText(),
				greenValueField.getText(), blueValueField.getText());
		
		if(newColor == null || newColor.equals(color))
			return;
		
		color = newColor;
		ignoreChanges = true;
		hexValueField.setText(ColorUtils.toHex(color).substring(1));
		redValueField.setText("" + color.getRed());
		greenValueField.setText("" + color.getGreen());
		blueValueField.setText("" + color.getBlue());
		ignoreChanges = false;
	}
	
	private void done()
	{
		colorSetting.setColor(color);
		WurstClient.setScreen(prevScreen);
	}
	
	@Override
	public void tick()
	{
		hexValueField.tick();
		redValueField.tick();
		greenValueField.tick();
		blueValueField.tick();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		TextRenderer tr = client.textRenderer;
		
		renderBackground(matrixStack);
		drawCenteredText(matrixStack, client.textRenderer,
			colorSetting.getName(), width / 2, 16, 0xF0F0F0);
		
		// Draw palette
		RenderSystem.setShaderTexture(0, paletteIdentifier);
		int x = paletteX;
		int y = paletteY;
		int w = paletteWidth;
		int h = paletteHeight;
		int fw = paletteWidth;
		int fh = paletteHeight;
		float u = 0;
		float v = 0;
		drawTexture(matrixStack, x, y, u, v, w, h, fw, fh);
		
		// RGB letters
		tr.draw(matrixStack, "#", fieldsX - 3 - tr.getWidth("#"), fieldsY + 6,
			0xF0F0F0);
		tr.draw(matrixStack, "R:", fieldsX - 3 - tr.getWidth("R:"),
			fieldsY + 6 + 35, 0xFF0000);
		tr.draw(matrixStack, "G:", fieldsX + 75 - 3 - tr.getWidth("G:"),
			fieldsY + 6 + 35, 0x00FF00);
		tr.draw(matrixStack, "B:", fieldsX + 150 - 3 - tr.getWidth("B:"),
			fieldsY + 6 + 35, 0x0000FF);
		
		hexValueField.render(matrixStack, mouseX, mouseY, partialTicks);
		redValueField.render(matrixStack, mouseX, mouseY, partialTicks);
		greenValueField.render(matrixStack, mouseX, mouseY, partialTicks);
		blueValueField.render(matrixStack, mouseX, mouseY, partialTicks);
		
		// Color preview
		
		int borderSize = 1;
		int boxWidth = 92;
		int boxHeight = 20;
		int boxX = width / 2 + 8;
		int boxY = fieldsY;
		
		// Border
		fill(matrixStack, boxX - borderSize, boxY - borderSize,
			boxX + boxWidth + borderSize, boxY + boxHeight + borderSize,
			0xFFAAAAAA);
		
		// Color box
		fill(matrixStack, boxX, boxY, boxX + boxWidth, boxY + boxHeight,
			color.getRGB());
		
		super.render(matrixStack, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void resize(MinecraftClient client, int width, int height)
	{
		String hex = hexValueField.getText();
		String r = redValueField.getText();
		String g = greenValueField.getText();
		String b = blueValueField.getText();
		
		init(client, width, height);
		
		hexValueField.setText(hex);
		redValueField.setText(r);
		greenValueField.setText(g);
		blueValueField.setText(b);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		switch(keyCode)
		{
			case GLFW.GLFW_KEY_ENTER:
			done();
			break;
			
			case GLFW.GLFW_KEY_ESCAPE:
				WurstClient.setScreen(prevScreen);
			break;
		}
		
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(mouseX >= paletteX && mouseX <= paletteX + paletteWidth
			&& mouseY >= paletteY && mouseY <= paletteY + paletteHeight)
		{
			if(paletteAsBufferedImage == null)
				return super.mouseClicked(mouseX, mouseY, button);
			
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
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	private void setColor(Color color)
	{
		hexValueField.setText(ColorUtils.toHex(color).substring(1));
		redValueField.setText("" + color.getRed());
		greenValueField.setText("" + color.getGreen());
		blueValueField.setText("" + color.getBlue());
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
