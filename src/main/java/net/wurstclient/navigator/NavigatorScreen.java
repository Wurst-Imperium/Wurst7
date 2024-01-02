/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.awt.Rectangle;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.wurstclient.WurstClient;

public abstract class NavigatorScreen extends Screen
{
	protected int scroll = 0;
	private int scrollKnobPosition = 2;
	private boolean scrolling;
	private int maxScroll;
	protected boolean scrollbarLocked;
	protected int middleX;
	protected boolean hasBackground = true;
	protected int nonScrollableArea = 26;
	private boolean showScrollbar;
	
	public NavigatorScreen()
	{
		super(Text.literal(""));
	}
	
	@Override
	protected final void init()
	{
		middleX = width / 2;
		onResize();
	}
	
	@Override
	public final boolean keyPressed(int keyCode, int scanCode, int int_3)
	{
		onKeyPress(keyCode, scanCode, int_3);
		return super.keyPressed(keyCode, scanCode, int_3);
	}
	
	@Override
	public final boolean mouseClicked(double x, double y, int button)
	{
		// scrollbar
		if(new Rectangle(width / 2 + 170, 60, 12, height - 103).contains(x, y))
			scrolling = true;
		
		onMouseClick(x, y, button);
		
		// vanilla buttons
		return super.mouseClicked(x, y, button);
	}
	
	@Override
	public final boolean mouseDragged(double mouseX, double mouseY,
		int mouseButton, double double_3, double double_4)
	{
		// scrollbar
		if(scrolling && !scrollbarLocked && mouseButton == 0)
		{
			if(maxScroll == 0)
				scroll = 0;
			else
				scroll = (int)((mouseY - 72) * maxScroll / (height - 131));
			
			if(scroll > 0)
				scroll = 0;
			else if(scroll < maxScroll)
				scroll = maxScroll;
			
			if(maxScroll == 0)
				scrollKnobPosition = 0;
			else
				scrollKnobPosition =
					(int)((height - 131) * scroll / (float)maxScroll);
			scrollKnobPosition += 2;
		}
		
		onMouseDrag(mouseX, mouseY, mouseButton, double_3, double_4);
		
		return super.mouseDragged(mouseX, mouseY, mouseButton, double_3,
			double_4);
	}
	
	@Override
	public final boolean mouseReleased(double x, double y, int button)
	{
		// scrollbar
		scrolling = false;
		
		onMouseRelease(x, y, button);
		
		// vanilla buttons
		return super.mouseReleased(x, y, button);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		// scrollbar
		if(!scrollbarLocked)
		{
			scroll += verticalAmount * 4;
			
			if(scroll > 0)
				scroll = 0;
			else if(scroll < maxScroll)
				scroll = maxScroll;
			
			if(maxScroll == 0)
				scrollKnobPosition = 0;
			else
				scrollKnobPosition =
					(int)((height - 131) * scroll / (float)maxScroll);
			scrollKnobPosition += 2;
		}
		
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount,
			verticalAmount);
	}
	
	@Override
	public final void tick()
	{
		onUpdate();
	}
	
	@Override
	public final void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		MatrixStack matrixStack = context.getMatrices();
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		// background
		int bgx1 = middleX - 154;
		int bgx2 = middleX + 154;
		int bgy1 = 60;
		int bgy2 = height - 43;
		if(hasBackground)
			drawBackgroundBox(matrixStack, bgx1, bgy1, bgx2, bgy2);
		
		// scrollbar
		if(showScrollbar)
		{
			// bar
			int x1 = bgx2 + 16;
			int x2 = x1 + 12;
			int y1 = bgy1;
			int y2 = bgy2;
			drawBackgroundBox(matrixStack, x1, y1, x2, y2);
			
			// knob
			x1 += 2;
			x2 -= 2;
			y1 += scrollKnobPosition;
			y2 = y1 + 24;
			drawForegroundBox(matrixStack, x1, y1, x2, y2);
			int i;
			for(x1++, x2--, y1 += 8, y2 -= 15, i = 0; i < 3; y1 += 4, y2 +=
				4, i++)
				drawDownShadow(matrixStack, x1, y1, x2, y2);
		}
		
		onRender(context, mouseX, mouseY, partialTicks);
		
		// GL resets
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	@Override
	public final boolean shouldPause()
	{
		return false;
	}
	
	protected abstract void onResize();
	
	protected abstract void onKeyPress(int keyCode, int scanCode, int int_3);
	
	protected abstract void onMouseClick(double x, double y, int button);
	
	protected abstract void onMouseDrag(double mouseX, double mouseY,
		int button, double double_3, double double_4);
	
	protected abstract void onMouseRelease(double x, double y, int button);
	
	protected abstract void onUpdate();
	
	protected abstract void onRender(DrawContext context, int mouseX,
		int mouseY, float partialTicks);
	
	protected final int getStringHeight(String s)
	{
		int fontHeight = client.textRenderer.fontHeight;
		int height = fontHeight;
		
		for(int i = 0; i < s.length(); i++)
			if(s.charAt(i) == '\n')
				height += fontHeight;
			
		return height;
	}
	
	protected final void setContentHeight(int contentHeight)
	{
		maxScroll = height - contentHeight - nonScrollableArea - 120;
		if(maxScroll > 0)
			maxScroll = 0;
		showScrollbar = maxScroll != 0;
		
		if(scroll < maxScroll)
			scroll = maxScroll;
	}
	
	protected final void drawQuads(MatrixStack matrixStack, int x1, int y1,
		int x2, int y2)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		tessellator.draw();
	}
	
	protected final void drawBoxShadow(MatrixStack matrixStack, int x1, int y1,
		int x2, int y2)
	{
		// color
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		// outline positions
		float xi1 = x1 - 0.1F;
		float xi2 = x2 + 0.1F;
		float yi1 = y1 - 0.1F;
		float yi2 = y2 + 0.1F;
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// outline
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, xi1, yi1, 0).next();
		bufferBuilder.vertex(matrix, xi2, yi1, 0).next();
		bufferBuilder.vertex(matrix, xi2, yi2, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi2, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi1, 0).next();
		tessellator.draw();
		
		// shadow positions
		xi1 -= 0.9;
		xi2 += 0.9;
		yi1 -= 0.9;
		yi2 += 0.9;
		
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_COLOR);
		
		// top
		bufferBuilder.vertex(matrix, x1, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, xi2, yi1, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi1, 0).color(0, 0, 0, 0).next();
		
		// left
		bufferBuilder.vertex(matrix, xi1, yi1, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x1, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		
		// right
		bufferBuilder.vertex(matrix, x2, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, xi2, yi1, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi2, yi2, 0).color(0, 0, 0, 0).next();
		
		// bottom
		bufferBuilder.vertex(matrix, xi2, yi2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, xi1, yi2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y2, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		
		tessellator.draw();
	}
	
	protected final void drawDownShadow(MatrixStack matrixStack, int x1, int y1,
		int x2, int y2)
	{
		// color
		float[] acColor = WurstClient.INSTANCE.getGui().getAcColor();
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// outline
		float yi1 = y1 + 0.1F;
		RenderSystem.setShaderColor(acColor[0], acColor[1], acColor[2], 0.5F);
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, yi1, 0).next();
		bufferBuilder.vertex(matrix, x2, yi1, 0).next();
		tessellator.draw();
		
		// shadow
		RenderSystem.setShader(GameRenderer::getPositionColorProgram);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_COLOR);
		bufferBuilder.vertex(matrix, x1, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y1, 0)
			.color(acColor[0], acColor[1], acColor[2], 0.75F).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).color(0, 0, 0, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).color(0, 0, 0, 0).next();
		tessellator.draw();
	}
	
	protected final void drawBox(MatrixStack matrixStack, int x1, int y1,
		int x2, int y2)
	{
		drawQuads(matrixStack, x1, y1, x2, y2);
		drawBoxShadow(matrixStack, x1, y1, x2, y2);
	}
	
	protected final void setColorToBackground()
	{
		WurstClient.INSTANCE.getGui().updateColors();
		float[] bgColor = WurstClient.INSTANCE.getGui().getBgColor();
		float opacity = WurstClient.INSTANCE.getGui().getOpacity();
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
	}
	
	protected final void setColorToForeground()
	{
		WurstClient.INSTANCE.getGui().updateColors();
		float[] bgColor = WurstClient.INSTANCE.getGui().getBgColor();
		float opacity = WurstClient.INSTANCE.getGui().getOpacity();
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
	}
	
	protected final void drawBackgroundBox(MatrixStack matrixStack, int x1,
		int y1, int x2, int y2)
	{
		setColorToBackground();
		drawBox(matrixStack, x1, y1, x2, y2);
	}
	
	protected final void drawForegroundBox(MatrixStack matrixStack, int x1,
		int y1, int x2, int y2)
	{
		setColorToForeground();
		drawBox(matrixStack, x1, y1, x2, y2);
	}
}
