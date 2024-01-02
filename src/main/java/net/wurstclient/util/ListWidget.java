/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public abstract class ListWidget extends AbstractParentElement
	implements Drawable
{
	protected final MinecraftClient client;
	protected int width;
	protected int height;
	protected int top;
	protected int bottom;
	protected int right;
	protected int left;
	protected final int itemHeight;
	protected boolean centerListVertically = true;
	protected int yDrag = -2;
	protected double scrollAmount;
	protected boolean visible = true;
	protected boolean renderSelection = true;
	protected boolean renderHeader;
	protected int headerHeight;
	private boolean scrolling;
	
	public ListWidget(MinecraftClient client, int width, int height, int top,
		int bottom, int itemHeight)
	{
		this.client = client;
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = bottom;
		this.itemHeight = itemHeight;
		left = 0;
		right = width;
	}
	
	public boolean isVisible()
	{
		return visible;
	}
	
	protected abstract int getItemCount();
	
	@Override
	public List<? extends Element> children()
	{
		return Collections.emptyList();
	}
	
	protected boolean selectItem(int index, int button, double mouseX,
		double mouseY)
	{
		return true;
	}
	
	protected abstract boolean isSelectedItem(int index);
	
	protected int getMaxPosition()
	{
		return getItemCount() * itemHeight + headerHeight;
	}
	
	protected abstract void renderBackground();
	
	protected void updateItemPosition(int index, int x, int y, float delta)
	{}
	
	protected abstract void renderItem(DrawContext context, int x, int y,
		int itemHeight, int mouseX, int mouseY, int i, float f);
	
	protected void renderHeader(int x, int y, Tessellator tessellator)
	{}
	
	protected void clickedHeader(int i, int j)
	{}
	
	protected void renderDecorations(int mouseX, int mouseY)
	{}
	
	public int getItemAtPosition(double mouseX, double mouseY)
	{
		int i = left + width / 2 - getRowWidth() / 2;
		int j = left + width / 2 + getRowWidth() / 2;
		int k = MathHelper.floor(mouseY - top) - headerHeight
			+ (int)scrollAmount - 4;
		int l = k / itemHeight;
		return mouseX < getScrollbarPosition() && mouseX >= i && mouseX <= j
			&& l >= 0 && k >= 0 && l < getItemCount() ? l : -1;
	}
	
	protected void capYPosition()
	{
		scrollAmount = MathHelper.clamp(scrollAmount, 0.0D, getMaxScroll());
	}
	
	public int getMaxScroll()
	{
		return Math.max(0, getMaxPosition() - (bottom - top - 4));
	}
	
	public boolean isMouseInList(double mouseX, double mouseY)
	{
		return mouseY >= top && mouseY <= bottom && mouseX >= left
			&& mouseX <= right;
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta)
	{
		if(visible)
		{
			renderBackground();
			int i = getScrollbarPosition();
			int j = i + 6;
			capYPosition();
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION_TEXTURE_COLOR);
			bufferBuilder.vertex(left, bottom, 0.0D)
				.texture(left / 32.0F, (bottom + (int)scrollAmount) / 32.0F)
				.color(32, 32, 32, 255).next();
			bufferBuilder.vertex(right, bottom, 0.0D)
				.texture(right / 32.0F, (bottom + (int)scrollAmount) / 32.0F)
				.color(32, 32, 32, 255).next();
			bufferBuilder.vertex(right, top, 0.0D)
				.texture(right / 32.0F, (top + (int)scrollAmount) / 32.0F)
				.color(32, 32, 32, 255).next();
			bufferBuilder.vertex(left, top, 0.0D)
				.texture(left / 32.0F, (top + (int)scrollAmount) / 32.0F)
				.color(32, 32, 32, 255).next();
			tessellator.draw();
			int k = left + width / 2 - getRowWidth() / 2 + 2;
			int l = top + 4 - (int)scrollAmount;
			if(renderHeader)
				renderHeader(k, l, tessellator);
			
			renderList(context, k, l, mouseX, mouseY, delta);
			RenderSystem.disableDepthTest();
			renderHoleBackground(0, top, 255, 255);
			renderHoleBackground(bottom, height, 255, 255);
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA,
				GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
			// RenderSystem.disableAlphaTest();
			// RenderSystem.shadeModel(7425);
			// RenderSystem.disableTexture();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION_TEXTURE_COLOR);
			bufferBuilder.vertex(left, top + 4, 0.0D).texture(0.0F, 1.0F)
				.color(0, 0, 0, 0).next();
			bufferBuilder.vertex(right, top + 4, 0.0D).texture(1.0F, 1.0F)
				.color(0, 0, 0, 0).next();
			bufferBuilder.vertex(right, top, 0.0D).texture(1.0F, 0.0F)
				.color(0, 0, 0, 255).next();
			bufferBuilder.vertex(left, top, 0.0D).texture(0.0F, 0.0F)
				.color(0, 0, 0, 255).next();
			tessellator.draw();
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
				VertexFormats.POSITION_TEXTURE_COLOR);
			bufferBuilder.vertex(left, bottom, 0.0D).texture(0.0F, 1.0F)
				.color(0, 0, 0, 255).next();
			bufferBuilder.vertex(right, bottom, 0.0D).texture(1.0F, 1.0F)
				.color(0, 0, 0, 255).next();
			bufferBuilder.vertex(right, bottom - 4, 0.0D).texture(1.0F, 0.0F)
				.color(0, 0, 0, 0).next();
			bufferBuilder.vertex(left, bottom - 4, 0.0D).texture(0.0F, 0.0F)
				.color(0, 0, 0, 0).next();
			tessellator.draw();
			int n = getMaxScroll();
			if(n > 0)
			{
				int o = (int)((float)((bottom - top) * (bottom - top))
					/ (float)getMaxPosition());
				o = MathHelper.clamp(o, 32, bottom - top - 8);
				int p = (int)scrollAmount * (bottom - top - o) / n + top;
				if(p < top)
					p = top;
				
				RenderSystem.setShader(GameRenderer::getPositionColorProgram);
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(i, bottom, 0.0D).color(0, 0, 0, 255)
					.next();
				bufferBuilder.vertex(j, bottom, 0.0D).color(0, 0, 0, 255)
					.next();
				bufferBuilder.vertex(j, top, 0.0D).color(0, 0, 0, 255).next();
				bufferBuilder.vertex(i, top, 0.0D).color(0, 0, 0, 255).next();
				tessellator.draw();
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(i, p + o, 0.0D).color(128, 128, 128, 255)
					.next();
				bufferBuilder.vertex(j, p + o, 0.0D).color(128, 128, 128, 255)
					.next();
				bufferBuilder.vertex(j, p, 0.0D).color(128, 128, 128, 255)
					.next();
				bufferBuilder.vertex(i, p, 0.0D).color(128, 128, 128, 255)
					.next();
				tessellator.draw();
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(i, p + o - 1, 0.0D)
					.color(192, 192, 192, 255).next();
				bufferBuilder.vertex(j - 1, p + o - 1, 0.0D)
					.color(192, 192, 192, 255).next();
				bufferBuilder.vertex(j - 1, p, 0.0D).color(192, 192, 192, 255)
					.next();
				bufferBuilder.vertex(i, p, 0.0D).color(192, 192, 192, 255)
					.next();
				tessellator.draw();
			}
			
			renderDecorations(mouseX, mouseY);
			// RenderSystem.enableTexture();
			// RenderSystem.shadeModel(7424);
			// RenderSystem.enableAlphaTest();
			RenderSystem.disableBlend();
		}
	}
	
	protected void updateScrollingState(double mouseX, double mouseY,
		int button)
	{
		scrolling = button == 0 && mouseX >= getScrollbarPosition()
			&& mouseX < getScrollbarPosition() + 6;
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		updateScrollingState(mouseX, mouseY, button);
		if(!isVisible() || !isMouseInList(mouseX, mouseY))
			return false;
		int i = getItemAtPosition(mouseX, mouseY);
		if(i == -1 && button == 0)
		{
			clickedHeader(
				(int)(mouseX - (left + width / 2 - getRowWidth() / 2)),
				(int)(mouseY - top) + (int)scrollAmount - 4);
			return true;
		}
		if(i == -1 || !selectItem(i, button, mouseX, mouseY))
			return scrolling;
		if(children().size() > i)
			setFocused(children().get(i));
		
		setDragging(true);
		return true;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		if(getFocused() != null)
			getFocused().mouseReleased(mouseX, mouseY, button);
		
		return false;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
		double deltaX, double deltaY)
	{
		if(super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
			return true;
		if(!isVisible() || button != 0 || !scrolling)
			return false;
		if(mouseY < top)
			scrollAmount = 0.0D;
		else if(mouseY > bottom)
			scrollAmount = getMaxScroll();
		else
		{
			double d = getMaxScroll();
			if(d < 1.0D)
				d = 1.0D;
			
			int i = (int)((float)((bottom - top) * (bottom - top))
				/ (float)getMaxPosition());
			i = MathHelper.clamp(i, 32, bottom - top - 8);
			double e = d / (bottom - top - i);
			if(e < 1.0D)
				e = 1.0D;
			
			scrollAmount += deltaY * e;
			capYPosition();
		}
		
		return true;
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY,
		double horizontalAmount, double verticalAmount)
	{
		if(!isVisible())
			return false;
		scrollAmount -= verticalAmount * itemHeight / 2.0D;
		return true;
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(!isVisible())
			return false;
		if(super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if(keyCode == 264)
		{
			moveSelection(1);
			return true;
		}
		if(keyCode == 265)
		{
			moveSelection(-1);
			return true;
		}
		return false;
	}
	
	protected void moveSelection(int by)
	{}
	
	@Override
	public boolean charTyped(char chr, int keyCode)
	{
		return !isVisible() ? false : super.charTyped(chr, keyCode);
	}
	
	@Override
	public boolean isMouseOver(double mouseX, double mouseY)
	{
		return isMouseInList(mouseX, mouseY);
	}
	
	public int getRowWidth()
	{
		return 220;
	}
	
	protected void renderList(DrawContext context, int i, int j, int k, int l,
		float f)
	{
		int m = getItemCount();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		
		for(int n = 0; n < m; ++n)
		{
			int o = j + n * itemHeight + headerHeight;
			int p = itemHeight - 4;
			if(o > bottom || o + p < top)
				updateItemPosition(n, i, o, f);
			
			if(renderSelection && isSelectedItem(n))
			{
				int q = left + width / 2 - getRowWidth() / 2;
				int r = left + width / 2 + getRowWidth() / 2;
				// RenderSystem.disableTexture();
				float g = isFocused() ? 1.0F : 0.5F;
				RenderSystem.setShaderColor(g, g, g, 1.0F);
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION);
				bufferBuilder.vertex(q, o + p + 2, 0.0D).next();
				bufferBuilder.vertex(r, o + p + 2, 0.0D).next();
				bufferBuilder.vertex(r, o - 2, 0.0D).next();
				bufferBuilder.vertex(q, o - 2, 0.0D).next();
				tessellator.draw();
				RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
					VertexFormats.POSITION);
				bufferBuilder.vertex(q + 1, o + p + 1, 0.0D).next();
				bufferBuilder.vertex(r - 1, o + p + 1, 0.0D).next();
				bufferBuilder.vertex(r - 1, o - 1, 0.0D).next();
				bufferBuilder.vertex(q + 1, o - 1, 0.0D).next();
				tessellator.draw();
				// RenderSystem.enableTexture();
			}
			
			RenderSystem.setShaderColor(1, 1, 1, 1);
			renderItem(context, n, i, o, p, k, l, f);
		}
		
	}
	
	@Override
	public boolean isFocused()
	{
		return false;
	}
	
	protected int getScrollbarPosition()
	{
		return width / 2 + 124;
	}
	
	protected void renderHoleBackground(int top, int bottom, int topAlpha,
		int bottomAlpha)
	{
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION_TEXTURE_COLOR);
		bufferBuilder.vertex(left, bottom, 0.0D).texture(0.0F, bottom / 32.0F)
			.color(64, 64, 64, bottomAlpha).next();
		bufferBuilder.vertex(left + width, bottom, 0.0D)
			.texture(width / 32.0F, bottom / 32.0F)
			.color(64, 64, 64, bottomAlpha).next();
		bufferBuilder.vertex(left + width, top, 0.0D)
			.texture(width / 32.0F, top / 32.0F).color(64, 64, 64, topAlpha)
			.next();
		bufferBuilder.vertex(left, top, 0.0D).texture(0.0F, top / 32.0F)
			.color(64, 64, 64, topAlpha).next();
		tessellator.draw();
	}
	
	public double getScrollAmount()
	{
		return scrollAmount;
	}
	
	protected void drawSelectionOutline(MatrixStack matrixStack, int x, int y)
	{
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x - 2, y - 2, 0).next();
		bufferBuilder.vertex(matrix, x + 218, y - 2, 0).next();
		bufferBuilder.vertex(matrix, x + 218, y + 28, 0).next();
		bufferBuilder.vertex(matrix, x - 2, y + 28, 0).next();
		tessellator.draw();
		
		RenderSystem.setShaderColor(0, 0, 0, 1);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x - 1, y - 1, 0).next();
		bufferBuilder.vertex(matrix, x + 217, y - 1, 0).next();
		bufferBuilder.vertex(matrix, x + 217, y + 27, 0).next();
		bufferBuilder.vertex(matrix, x - 1, y + 27, 0).next();
		tessellator.draw();
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
