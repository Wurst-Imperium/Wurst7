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
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.SelectFileScreen;
import net.wurstclient.settings.FileSetting;

public final class FileComponent extends Component
{
	private final FileSetting setting;
	
	public FileComponent(FileSetting setting)
	{
		this.setting = setting;
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		TextRenderer fr = WurstClient.MC.textRenderer;
		int buttonWidth = fr.getWidth(setting.getSelectedFileName());
		
		if(mouseX < getX() + getWidth() - buttonWidth - 4)
			return;
		
		WurstClient.MC.setScreen(
			new SelectFileScreen(WurstClient.MC.currentScreen, setting));
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		int txtColor = gui.getTxtColor();
		float opacity = gui.getOpacity();
		
		TextRenderer tr = WurstClient.MC.textRenderer;
		int buttonWidth = tr.getWidth(setting.getSelectedFileName());
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - buttonWidth - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		int scroll = getParent().isScrollingEnabled()
			? getParent().getScrollOffset() : 0;
		boolean hovering = mouseX >= x1 && mouseY >= y1 && mouseX < x2
			&& mouseY < y2 && mouseY >= -scroll
			&& mouseY < getParent().getHeight() - 13 - scroll;
		boolean hText = hovering && mouseX < x3;
		boolean hBox = hovering && mouseX >= x3;
		
		MatrixStack matrixStack = context.getMatrices();
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// tooltip
		if(hText)
			gui.setTooltip(setting.getWrappedDescription(200));
		else if(hBox)
		{
			String tooltip = "\u00a7e[left-click]\u00a7r to select file";
			gui.setTooltip(tooltip);
		}
		
		// background
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		tessellator.draw();
		
		// box
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			hBox ? opacity * 1.5F : opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		tessellator.draw();
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		bufferBuilder.vertex(matrix, x3, y1, 0).next();
		tessellator.draw();
		
		// setting name
		RenderSystem.setShaderColor(1, 1, 1, 1);
		String text = setting.getName() + ": ";
		context.drawText(tr, text, x1, y1 + 2, txtColor, false);
		context.drawText(tr, setting.getSelectedFileName(), x3 + 2, y1 + 2,
			txtColor, false);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer fr = WurstClient.MC.textRenderer;
		
		String text = setting.getName() + ": ";
		int buttonWidth = fr.getWidth(setting.getSelectedFileName());
		
		return fr.getWidth(text) + buttonWidth + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
