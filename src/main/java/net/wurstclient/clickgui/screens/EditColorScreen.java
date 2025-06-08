/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.screens;

import java.awt.Color;
import java.util.function.Consumer;

import net.wurstclient.util.RenderUtils;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.ColorUtils;

public final class EditColorScreen extends Screen
{
	private final Screen prevScreen;
	private final ColorSetting colorSetting;
	private Color color;
	
	private TextFieldWidget hexValueField;
	private TextFieldWidget redValueField;
	private TextFieldWidget greenValueField;
	private TextFieldWidget blueValueField;
	
	private ColorPickerWidget colorPicker;
	
	private int fieldsX = 0;
	private int fieldsY = 0;
	
	private boolean ignoreChanges;
	
	public EditColorScreen(Screen prevScreen, ColorSetting colorSetting)
	{
		super(Text.literal(""));
		this.prevScreen = prevScreen;
		this.colorSetting = colorSetting;
		color = colorSetting.getColor();
	}
	
	@Override
	public void init()
	{
		TextRenderer tr = client.textRenderer;
		
		int pickerWidth = 200;
		int pickerHeight = 100;
		int pickerX = width / 2 - 100;
		int pickerY = 32;
		
		fieldsX = pickerX;
		fieldsY = pickerY + pickerHeight + 15;
		
		colorPicker = new ColorPickerWidget(pickerX, pickerY, pickerWidth,
			pickerHeight, color, this::setColorFromPicker);
		
		hexValueField =
			new TextFieldWidget(tr, fieldsX, fieldsY, 92, 20, Text.literal(""));
		hexValueField.setMaxLength(6);
		hexValueField.setChangedListener(s -> updateColorFromTextFields(true));
		
		redValueField = new TextFieldWidget(tr, fieldsX, fieldsY + 35, 50, 20,
			Text.literal(""));
		redValueField.setMaxLength(3);
		redValueField.setChangedListener(s -> updateColorFromTextFields(false));
		
		greenValueField = new TextFieldWidget(tr, fieldsX + 75, fieldsY + 35,
			50, 20, Text.literal(""));
		greenValueField.setMaxLength(3);
		greenValueField
			.setChangedListener(s -> updateColorFromTextFields(false));
		
		blueValueField = new TextFieldWidget(tr, fieldsX + 150, fieldsY + 35,
			50, 20, Text.literal(""));
		blueValueField.setMaxLength(3);
		blueValueField
			.setChangedListener(s -> updateColorFromTextFields(false));
		
		updateTextFields();
		
		addDrawableChild(hexValueField);
		addDrawableChild(redValueField);
		addDrawableChild(greenValueField);
		addDrawableChild(blueValueField);
		
		setFocused(hexValueField);
		hexValueField.setFocused(true);
		hexValueField.setSelectionStart(0);
		hexValueField.setSelectionEnd(6);
		
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> done())
			.dimensions(pickerX, height - 30, 200, 20).build());
	}
	
	private void updateColorFromTextFields(boolean hex)
	{
		if(ignoreChanges)
			return;
		
		Color newColor =
			hex ? ColorUtils.tryParseHex("#" + hexValueField.getText())
				: ColorUtils.tryParseRGB(redValueField.getText(),
					greenValueField.getText(), blueValueField.getText());
		
		if(newColor != null && !newColor.equals(color))
		{
			this.color = newColor;
			ignoreChanges = true;
			updateTextFields();
			colorPicker.setColor(newColor);
			ignoreChanges = false;
		}
	}
	
	private void setColorFromPicker(Color newColor)
	{
		if(ignoreChanges || newColor.equals(color))
			return;
		
		this.color = newColor;
		ignoreChanges = true;
		updateTextFields();
		ignoreChanges = false;
	}
	
	private void updateTextFields()
	{
		hexValueField.setText(ColorUtils.toHex(color).substring(1));
		redValueField.setText(String.valueOf(color.getRed()));
		greenValueField.setText(String.valueOf(color.getGreen()));
		blueValueField.setText(String.valueOf(color.getBlue()));
	}
	
	private void done()
	{
		colorSetting.setColor(color);
		close();
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		renderBackground(context, mouseX, mouseY, partialTicks);
		context.drawCenteredTextWithShadow(client.textRenderer,
			colorSetting.getName(), width / 2, 16, 0xF0F0F0);
		
		colorPicker.render(context);
		
		TextRenderer tr = client.textRenderer;
		
		// RGB labels
		context.drawText(tr, "#", fieldsX - 3 - tr.getWidth("#"), fieldsY + 6,
			0xF0F0F0, false);
		context.drawText(tr, "R:", fieldsX - 3 - tr.getWidth("R:"),
			fieldsY + 6 + 35, 0xFF0000, false);
		context.drawText(tr, "G:", fieldsX + 75 - 3 - tr.getWidth("G:"),
			fieldsY + 6 + 35, 0x00FF00, false);
		context.drawText(tr, "B:", fieldsX + 150 - 3 - tr.getWidth("B:"),
			fieldsY + 6 + 35, 0x0000FF, false);
		
		// Color preview
		int borderSize = 1;
		int boxWidth = 92;
		int boxHeight = 18;
		int boxX = width / 2 + 7;
		int boxY = fieldsY + 1;
		
		context.fill(boxX - borderSize, boxY - borderSize,
			boxX + boxWidth + borderSize, boxY + boxHeight + borderSize,
			0xFFAAAAAA);
		context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight,
			color.getRGB());
		
		for(Drawable drawable : drawables)
			drawable.render(context, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		if(colorPicker.mouseClicked(mouseX, mouseY, button))
			return true;
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		colorPicker.mouseReleased();
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
		double deltaX, double deltaY)
	{
		if(colorPicker.mouseDragged(mouseX, mouseY, button))
			return true;
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
		{
			done();
			return true;
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean shouldPause()
	{
		return false;
	}
	
	@Override
	public void close()
	{
		client.setScreen(prevScreen);
	}
	
	private static class ColorPickerWidget
	{
		private static final int[] HUE_COLORS =
			new int[]{0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
				0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
		
		private final int x;
		private final int y;
		private final int height;
		
		private final int pickerWidth;
		private final int hueBarWidth;
		private final int gap;
		
		private final Consumer<Color> colorConsumer;
		
		private float hue;
		private float saturation;
		private float brightness;
		
		private boolean isDraggingPicker;
		private boolean isDraggingHue;
		
		ColorPickerWidget(int x, int y, int width, int height,
			Color initialColor, Consumer<Color> colorConsumer)
		{
			this.x = x;
			this.y = y;
			this.height = height;
			this.colorConsumer = colorConsumer;
			
			this.hueBarWidth = 20;
			this.gap = 15;
			this.pickerWidth = width - hueBarWidth - gap;
			
			setColor(initialColor);
		}
		
		void render(DrawContext context)
		{
			drawPicker(context);
			drawHueBar(context);
			drawHandles(context);
		}
		
		private void drawPicker(DrawContext context)
		{
			Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
			Color hueColor = Color.getHSBColor(hue, 1.0f, 1.0f);
			
			// Layer 1: Horizontal gradient from White (Saturation 0)
			// to the fully saturated Hue (Saturation 1).
			context.draw(consumers -> {
				VertexConsumer buffer =
					consumers.getBuffer(RenderLayer.getGui());
				buffer.vertex(matrix, x, y, 0).color(0xFFFFFFFF);
				buffer.vertex(matrix, x, y + height, 0).color(0xFFFFFFFF);
				buffer.vertex(matrix, x + pickerWidth, y + height, 0)
					.color(hueColor.getRGB());
				buffer.vertex(matrix, x + pickerWidth, y, 0)
					.color(hueColor.getRGB());
			});
			
			// Layer 2: Vertical gradient from Transparent (Brightness 1)
			// to Black (Brightness 0).
			context.draw(consumers -> {
				VertexConsumer buffer =
					consumers.getBuffer(RenderLayer.getGui());
				buffer.vertex(matrix, x, y, 0).color(0x00000000);
				buffer.vertex(matrix, x, y + height, 0).color(0xFF000000);
				buffer.vertex(matrix, x + pickerWidth, y + height, 0)
					.color(0xFF000000);
				buffer.vertex(matrix, x + pickerWidth, y, 0).color(0x00000000);
			});
			
			context.drawBorder(x, y, pickerWidth, height, 0xFFAAAAAA);
		}
		
		private void drawHueBar(DrawContext context)
		{
			int hueBarX = x + pickerWidth + gap;
			Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
			
			context.draw(consumers -> {
				VertexConsumer buffer =
					consumers.getBuffer(RenderLayer.getGui());
				
				for(int i = 0; i < HUE_COLORS.length - 1; i++)
				{
					float segmentYStart =
						y + (height * i / (HUE_COLORS.length - 1f));
					float segmentYEnd =
						y + (height * (i + 1) / (HUE_COLORS.length - 1f));
					
					int colorStart = HUE_COLORS[i];
					int colorEnd = HUE_COLORS[i + 1];
					
					buffer.vertex(matrix, hueBarX, segmentYStart, 0)
						.color(colorStart);
					buffer.vertex(matrix, hueBarX, segmentYEnd, 0)
						.color(colorEnd);
					buffer.vertex(matrix, hueBarX + hueBarWidth, segmentYEnd, 0)
						.color(colorEnd);
					buffer
						.vertex(matrix, hueBarX + hueBarWidth, segmentYStart, 0)
						.color(colorStart);
				}
			});
			
			context.drawBorder(hueBarX, y, hueBarWidth, height, 0xFFAAAAAA);
		}
		
		private void drawHandles(DrawContext context)
		{
			// Picker Handle
			int pickerHandleX = (int)(x + (saturation * pickerWidth));
			int pickerHandleY = (int)(y + ((1 - brightness) * height));
			int radius = 3;
			int size = radius * 2;
			int handleX = pickerHandleX - radius;
			int handleY = pickerHandleY - radius;
			context.drawBorder(handleX + 1, handleY + 1, size, size,
				0xFF000000);
			context.drawBorder(handleX, handleY, size, size, 0xFFFFFFFF);
			
			// Hue Bar Handle
			int hueBarX = x + pickerWidth + gap;
			int hueHandleY = (int)(y + (hue * height));
			int triangleSize = 6;
			int triangleTop = hueHandleY - triangleSize / 2;
			RenderUtils.fillTriangle2D(context,
				new float[][]{{hueBarX - 4, triangleTop},
					{hueBarX - 4, triangleTop + triangleSize},
					{hueBarX - 1, hueHandleY}},
				0xFFFFFFFF);
		}
		
		boolean mouseClicked(double mouseX, double mouseY, int button)
		{
			if(button != 0)
				return false;
			
			int hueBarX = x + pickerWidth + gap;
			
			if(mouseX >= x && mouseX <= x + pickerWidth && mouseY >= y
				&& mouseY <= y + height)
			{
				isDraggingPicker = true;
				updatePicker(mouseX, mouseY);
				return true;
			}
			
			if(mouseX >= hueBarX && mouseX <= hueBarX + hueBarWidth
				&& mouseY >= y && mouseY <= y + height)
			{
				isDraggingHue = true;
				updateHue(mouseY);
				return true;
			}
			
			return false;
		}
		
		boolean mouseDragged(double mouseX, double mouseY, int button)
		{
			if(button != 0)
				return false;
			
			if(isDraggingPicker)
			{
				updatePicker(mouseX, mouseY);
				return true;
			}
			
			if(isDraggingHue)
			{
				updateHue(mouseY);
				return true;
			}
			
			return false;
		}
		
		void mouseReleased()
		{
			isDraggingPicker = false;
			isDraggingHue = false;
		}
		
		private void updatePicker(double mouseX, double mouseY)
		{
			saturation =
				(float)MathHelper.clamp((mouseX - x) / pickerWidth, 0.0, 1.0);
			brightness =
				1.0f - (float)MathHelper.clamp((mouseY - y) / height, 0.0, 1.0);
			updateColor();
		}
		
		private void updateHue(double mouseY)
		{
			hue = (float)MathHelper.clamp((mouseY - y) / height, 0.0, 1.0);
			updateColor();
		}
		
		private void updateColor()
		{
			Color newColor = Color.getHSBColor(hue, saturation, brightness);
			colorConsumer.accept(newColor);
		}
		
		void setColor(Color newColor)
		{
			float[] hsb = Color.RGBtoHSB(newColor.getRed(), newColor.getGreen(),
				newColor.getBlue(), null);
			this.hue = hsb[0];
			this.saturation = hsb[1];
			this.brightness = hsb[2];
		}
	}
}
