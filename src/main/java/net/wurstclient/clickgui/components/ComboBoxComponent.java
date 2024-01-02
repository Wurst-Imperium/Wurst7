/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.ComboBoxPopup;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.settings.EnumSetting;

public final class ComboBoxComponent<T extends Enum<T>> extends Component
{
	private final ClickGui gui = WurstClient.INSTANCE.getGui();
	private final TextRenderer tr = WurstClient.MC.textRenderer;
	
	private final EnumSetting<T> setting;
	private final int popupWidth;
	private ComboBoxPopup<T> popup;
	
	public ComboBoxComponent(EnumSetting<T> setting)
	{
		this.setting = setting;
		popupWidth = calculatePopupWitdh();
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	private int calculatePopupWitdh()
	{
		Stream<T> values = Arrays.stream(setting.getValues());
		Stream<String> vNames = values.map(T::toString);
		IntStream vWidths = vNames.mapToInt(s -> tr.getWidth(s));
		return vWidths.max().getAsInt();
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseX < getX() + getWidth() - popupWidth - 15)
			return;
		
		switch(mouseButton)
		{
			case 0:
			handleLeftClick();
			break;
			
			case 1:
			handleRightClick();
			break;
		}
	}
	
	private void handleLeftClick()
	{
		if(isPopupOpen())
		{
			popup.close();
			popup = null;
			return;
		}
		
		popup = new ComboBoxPopup<>(this, setting, popupWidth);
		gui.addPopup(popup);
	}
	
	private void handleRightClick()
	{
		if(isPopupOpen())
			return;
		
		T defaultSelected = setting.getDefaultSelected();
		setting.setSelected(defaultSelected);
	}
	
	private boolean isPopupOpen()
	{
		return popup != null && !popup.isClosing();
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - 11;
		int x4 = x3 - popupWidth - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		boolean hText = hovering && mouseX < x4;
		boolean hBox = hovering && mouseX >= x4;
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// tooltip
		if(hText)
			gui.setTooltip(setting.getWrappedDescription(200));
		
		drawBackground(matrixStack, x1, x4, y1, y2);
		drawBox(matrixStack, x2, x4, y1, y2, hBox);
		
		drawSeparator(matrixStack, x3, y1, y2);
		drawArrow(matrixStack, x2, x3, y1, y2, hBox);
		
		drawNameAndValue(context, x1, x4, y1);
	}
	
	private boolean isHovering(int mouseX, int mouseY, int x1, int x2, int y1,
		int y2)
	{
		Window parent = getParent();
		boolean scrollEnabled = parent.isScrollingEnabled();
		int scroll = scrollEnabled ? parent.getScrollOffset() : 0;
		
		return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2
			&& mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
	}
	
	private void drawBackground(MatrixStack matrixStack, int x1, int x4, int y1,
		int y2)
	{
		float[] bgColor = gui.getBgColor();
		float opacity = gui.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x4, y2, 0).next();
		bufferBuilder.vertex(matrix, x4, y1, 0).next();
		tessellator.draw();
	}
	
	private void drawBox(MatrixStack matrixStack, int x2, int x4, int y1,
		int y2, boolean hBox)
	{
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		// background
		float bgAlpha = hBox ? opacity * 1.5F : opacity;
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			bgAlpha);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x4, y1, 0).next();
		bufferBuilder.vertex(matrix, x4, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x4, y1, 0).next();
		bufferBuilder.vertex(matrix, x4, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		bufferBuilder.vertex(matrix, x4, y1, 0).next();
		tessellator.draw();
	}
	
	private void drawSeparator(MatrixStack matrixStack, int x3, int y1, int y2)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		tessellator.draw();
	}
	
	private void drawArrow(MatrixStack matrixStack, int x2, int x3, int y1,
		int y2, boolean hBox)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		float xa1 = x3 + 1;
		float xa2 = (x3 + x2) / 2.0F;
		float xa3 = x2 - 1;
		float ya1;
		float ya2;
		
		if(isPopupOpen())
		{
			ya1 = y2 - 3.5F;
			ya2 = y1 + 3;
			RenderSystem.setShaderColor(hBox ? 1 : 0.85F, 0, 0, 1);
			
		}else
		{
			ya1 = y1 + 3.5F;
			ya2 = y2 - 3;
			RenderSystem.setShaderColor(0, hBox ? 1 : 0.85F, 0, 1);
		}
		
		// arrow
		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLES,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(0.0625F, 0.0625F, 0.0625F, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa3, ya1, 0).next();
		bufferBuilder.vertex(matrix, xa2, ya2, 0).next();
		bufferBuilder.vertex(matrix, xa1, ya1, 0).next();
		tessellator.draw();
	}
	
	private void drawNameAndValue(DrawContext context, int x1, int x4, int y1)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		String name = setting.getName();
		String value = "" + setting.getSelected();
		
		context.drawText(tr, name, x1, y1 + 2, txtColor, false);
		context.drawText(tr, value, x4 + 2, y1 + 2, txtColor, false);
		
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return tr.getWidth(setting.getName()) + popupWidth + 17;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
