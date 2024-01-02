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
import net.minecraft.client.gui.DrawContext;
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
import net.wurstclient.settings.CheckboxSetting;

public final class CheckboxComponent extends Component
{
	private final MinecraftClient MC = WurstClient.MC;
	private final ClickGui GUI = WurstClient.INSTANCE.getGui();
	
	private final CheckboxSetting setting;
	
	public CheckboxComponent(CheckboxSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		switch(mouseButton)
		{
			case 0:
			setting.setChecked(!setting.isChecked());
			break;
			
			case 1:
			setting.setChecked(setting.isCheckedByDefault());
			break;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + 11;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		if(hovering && mouseX >= x3)
			setTooltip();
		
		if(setting.isLocked())
			hovering = false;
		
		drawBackground(matrixStack, x2, x3, y1, y2);
		drawBox(matrixStack, x1, x3, y1, y2, hovering);
		
		if(setting.isChecked())
			drawCheck(matrixStack, x1, y1, hovering);
		
		drawName(context, x3, y1);
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
			tooltip += "\n\nThis checkbox is locked to ";
			tooltip += setting.isChecked() + ".";
		}
		
		GUI.setTooltip(tooltip);
	}
	
	private void drawBackground(MatrixStack matrixStack, int x2, int x3, int y1,
		int y2)
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
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		tessellator.draw();
	}
	
	private void drawBox(MatrixStack matrixStack, int x1, int x3, int y1,
		int y2, boolean hovering)
	{
		float[] bgColor = GUI.getBgColor();
		float[] acColor = GUI.getAcColor();
		float opacity = GUI.getOpacity();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			hovering ? opacity * 1.5F : opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		tessellator.draw();
		
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		tessellator.draw();
	}
	
	private void drawCheck(MatrixStack matrixStack, int x1, int y1,
		boolean hovering)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		float xc1 = x1 + 2.5F;
		float xc2 = x1 + 3.5F;
		float xc3 = x1 + 4.5F;
		float xc4 = x1 + 7.5F;
		float xc5 = x1 + 8.5F;
		float yc1 = y1 + 2.5F;
		float yc2 = y1 + 3.5F;
		float yc3 = y1 + 5.5F;
		float yc4 = y1 + 6.5F;
		float yc5 = y1 + 8.5F;
		
		// check
		if(setting.isLocked())
			RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 0.75F);
		else
			RenderSystem.setShaderColor(0, hovering ? 1 : 0.85F, 0, 1);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xc2, yc3, 0).next();
		bufferBuilder.vertex(matrix, xc3, yc4, 0).next();
		bufferBuilder.vertex(matrix, xc3, yc5, 0).next();
		bufferBuilder.vertex(matrix, xc1, yc4, 0).next();
		bufferBuilder.vertex(matrix, xc4, yc1, 0).next();
		bufferBuilder.vertex(matrix, xc5, yc2, 0).next();
		bufferBuilder.vertex(matrix, xc3, yc5, 0).next();
		bufferBuilder.vertex(matrix, xc3, yc4, 0).next();
		tessellator.draw();
		
		// outline
		RenderSystem.setShaderColor(0.0625F, 0.0625F, 0.0625F, 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xc2, yc3, 0).next();
		bufferBuilder.vertex(matrix, xc3, yc4, 0).next();
		bufferBuilder.vertex(matrix, xc4, yc1, 0).next();
		bufferBuilder.vertex(matrix, xc5, yc2, 0).next();
		bufferBuilder.vertex(matrix, xc3, yc5, 0).next();
		bufferBuilder.vertex(matrix, xc1, yc4, 0).next();
		bufferBuilder.vertex(matrix, xc2, yc3, 0).next();
		tessellator.draw();
	}
	
	private void drawName(DrawContext context, int x3, int y1)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		String name = setting.getName();
		int tx = x3 + 2;
		int ty = y1 + 2;
		context.drawText(MC.textRenderer, name, tx, ty, txtColor, false);
		
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return MC.textRenderer.getWidth(setting.getName()) + 13;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
