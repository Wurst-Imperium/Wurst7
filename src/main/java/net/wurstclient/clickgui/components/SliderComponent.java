/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.screens.EditSliderScreen;
import net.wurstclient.settings.SliderSetting;

public final class SliderComponent extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
	private final SliderSetting setting;
	private boolean dragging;
	
	public SliderComponent(SliderSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseY < getY() + 11)
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
		if(Screen.hasControlDown())
			MC.setScreen(new EditSliderScreen(MC.currentScreen, setting));
		else
			dragging = true;
	}
	
	private void handleRightClick()
	{
		setting.setValue(setting.getDefaultValue());
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + 2;
		int x4 = x2 - 2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y1 + 11;
		int y4 = y3 + 4;
		int y5 = y2 - 4;
		
		handleDragging(mouseX, x3, x4);
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		boolean hSlider = hovering && mouseY >= y3 || dragging;
		boolean renderAsDisabled = setting.isDisabled() || setting.isLocked();
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		if(hovering && mouseY < y3)
			setTooltip();
		else if(hSlider && !dragging)
			GUI.setTooltip(
				"\u00a7e[ctrl]\u00a7r+\u00a7e[left-click]\u00a7r for precise input\n"
					+ "\u00a7e[right-click]\u00a7r to reset");
		
		if(renderAsDisabled)
		{
			hovering = false;
			hSlider = false;
		}
		
		drawBackground(matrixStack, x1, x2, x3, x4, y1, y2, y4, y5);
		drawRail(matrixStack, x3, x4, y4, y5, hSlider, renderAsDisabled);
		drawKnob(matrixStack, x1, x2, y2, y3, hSlider, renderAsDisabled);
		drawNameAndValue(context, x1, x2, y1, renderAsDisabled);
	}
	
	private void handleDragging(int mouseX, int x3, int x4)
	{
		if(!dragging)
			return;
		
		if(!GUI.isLeftMouseButtonPressed())
		{
			dragging = false;
			return;
		}
		
		double sliderStartX = x3;
		double sliderWidth = x4 - x3;
		double mousePercentage = (mouseX - sliderStartX) / sliderWidth;
		
		double min = setting.getMinimum();
		double range = setting.getRange();
		double value = min + range * mousePercentage;
		
		setting.setValue(value);
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
	
	private void setTooltip()
	{
		String tooltip = setting.getWrappedDescription(200);
		
		if(setting.isLocked())
		{
			tooltip += "\n\nThis slider is locked to ";
			tooltip += setting.getValueString() + ".";
			
		}else if(setting.isDisabled())
			tooltip += "\n\nThis slider is disabled.";
		
		GUI.setTooltip(tooltip);
	}
	
	private void drawBackground(MatrixStack matrixStack, int x1, int x2, int x3,
		int x4, int y1, int y2, int y4, int y5)
	{
		float[] bgColor = GUI.getBgColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y4, 0).next();
		bufferBuilder.vertex(matrix, x2, y4, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		
		bufferBuilder.vertex(matrix, x1, y5, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y5, 0).next();
		
		bufferBuilder.vertex(matrix, x1, y4, 0).next();
		bufferBuilder.vertex(matrix, x1, y5, 0).next();
		bufferBuilder.vertex(matrix, x3, y5, 0).next();
		bufferBuilder.vertex(matrix, x3, y4, 0).next();
		
		bufferBuilder.vertex(matrix, x4, y4, 0).next();
		bufferBuilder.vertex(matrix, x4, y5, 0).next();
		bufferBuilder.vertex(matrix, x2, y5, 0).next();
		bufferBuilder.vertex(matrix, x2, y4, 0).next();
		
		tessellator.draw();
	}
	
	private void drawRail(MatrixStack matrixStack, int x3, int x4, int y4,
		int y5, boolean hSlider, boolean renderAsDisabled)
	{
		float[] bgColor = GUI.getBgColor();
		float[] acColor = GUI.getAcColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		float xl1 = x3;
		float xl2 = x4;
		if(!renderAsDisabled && setting.isLimited())
		{
			double ratio = (x4 - x3) / setting.getRange();
			xl1 += ratio * (setting.getUsableMin() - setting.getMinimum());
			xl2 += ratio * (setting.getUsableMax() - setting.getMaximum());
		}
		
		// limit
		RenderSystem.setShaderColor(1, 0, 0,
			hSlider ? opacity * 1.5F : opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x3, y4, 0).next();
		bufferBuilder.vertex(matrix, x3, y5, 0).next();
		bufferBuilder.vertex(matrix, xl1, y5, 0).next();
		bufferBuilder.vertex(matrix, xl1, y4, 0).next();
		bufferBuilder.vertex(matrix, xl2, y4, 0).next();
		bufferBuilder.vertex(matrix, xl2, y5, 0).next();
		bufferBuilder.vertex(matrix, x4, y5, 0).next();
		bufferBuilder.vertex(matrix, x4, y4, 0).next();
		tessellator.draw();
		
		// background
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			hSlider ? opacity * 1.5F : opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xl1, y4, 0).next();
		bufferBuilder.vertex(matrix, xl1, y5, 0).next();
		bufferBuilder.vertex(matrix, xl2, y5, 0).next();
		bufferBuilder.vertex(matrix, xl2, y4, 0).next();
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x3, y4, 0).next();
		bufferBuilder.vertex(matrix, x3, y5, 0).next();
		bufferBuilder.vertex(matrix, x4, y5, 0).next();
		bufferBuilder.vertex(matrix, x4, y4, 0).next();
		bufferBuilder.vertex(matrix, x3, y4, 0).next();
		tessellator.draw();
	}
	
	private void drawKnob(MatrixStack matrixStack, int x1, int x2, int y2,
		int y3, boolean hSlider, boolean renderAsDisabled)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		double percentage = setting.getPercentage();
		float xk1 = x1 + (x2 - x1 - 8) * (float)percentage;
		float xk2 = xk1 + 8;
		float yk1 = y3 + 1.5F;
		float yk2 = y2 - 1.5F;
		
		// knob
		if(renderAsDisabled)
			RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 0.75F);
		else
		{
			float[] c = setting.getKnobColor();
			RenderSystem.setShaderColor(c[0], c[1], c[2], hSlider ? 1 : 0.75F);
		}
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xk1, yk1, 0).next();
		bufferBuilder.vertex(matrix, xk1, yk2, 0).next();
		bufferBuilder.vertex(matrix, xk2, yk2, 0).next();
		bufferBuilder.vertex(matrix, xk2, yk1, 0).next();
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(0.0625F, 0.0625F, 0.0625F, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xk1, yk1, 0).next();
		bufferBuilder.vertex(matrix, xk1, yk2, 0).next();
		bufferBuilder.vertex(matrix, xk2, yk2, 0).next();
		bufferBuilder.vertex(matrix, xk2, yk1, 0).next();
		bufferBuilder.vertex(matrix, xk1, yk1, 0).next();
		tessellator.draw();
	}
	
	private void drawNameAndValue(DrawContext context, int x1, int x2, int y1,
		boolean renderAsDisabled)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		TextRenderer tr = MC.textRenderer;
		String name = setting.getName();
		String value = setting.getValueString();
		int valueWidth = tr.getWidth(value);
		context.drawText(tr, name, x1, y1 + 2, txtColor, false);
		context.drawText(tr, value, x2 - valueWidth, y1 + 2, txtColor, false);
		
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer tr = MC.textRenderer;
		int nameWitdh = tr.getWidth(setting.getName());
		int valueWidth = tr.getWidth(setting.getValueString());
		return nameWitdh + valueWidth + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 22;
	}
}
