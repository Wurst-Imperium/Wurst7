/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Matrix4f;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.EditBlockScreen;
import net.wurstclient.settings.BlockSetting;

public final class BlockComponent extends Component
{
	private static final int BLOCK_WITDH = 24;
	private final BlockSetting setting;
	
	public BlockComponent(BlockSetting setting)
	{
		this.setting = setting;
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseX < getX() + getWidth() - BLOCK_WITDH)
			return;
		
		if(mouseButton == 0)
		{
			Screen currentScreen = WurstClient.MC.currentScreen;
			EditBlockScreen editScreen =
				new EditBlockScreen(currentScreen, setting);
			WurstClient.MC.openScreen(editScreen);
			
		}else if(mouseButton == 1)
			setting.resetToDefault();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float opacity = gui.getOpacity();
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - BLOCK_WITDH;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		int scroll = getParent().isScrollingEnabled()
			? getParent().getScrollOffset() : 0;
		boolean hovering = mouseX >= x1 && mouseY >= y1 && mouseX < x2
			&& mouseY < y2 && mouseY >= -scroll
			&& mouseY < getParent().getHeight() - 13 - scroll;
		boolean hText = hovering && mouseX < x3;
		boolean hBlock = hovering && mouseX >= x3;
		
		ItemStack stack = new ItemStack(setting.getBlock());
		
		Matrix4f matrix = matrixStack.peek().getModel();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.setShader(GameRenderer::method_34539);
		
		// tooltip
		if(hText)
			gui.setTooltip(setting.getDescription());
		else if(hBlock)
		{
			String tooltip = "\u00a76Name:\u00a7r " + getBlockName(stack);
			tooltip += "\n\u00a76ID:\u00a7r " + setting.getBlockName();
			tooltip += "\n\n\u00a7e[left-click]\u00a7r to edit";
			tooltip += "\n\u00a7e[right-click]\u00a7r to reset";
			gui.setTooltip(tooltip);
		}
		
		// background
		RenderSystem.setShaderColor(bgColor[0], bgColor[1], bgColor[2],
			opacity);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS,
			VertexFormats.POSITION);
		bufferBuilder.vertex(matrix, x1, y1, 0).next();
		bufferBuilder.vertex(matrix, x1, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y2, 0).next();
		bufferBuilder.vertex(matrix, x2, y1, 0).next();
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		
		// setting name
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TextRenderer fr = WurstClient.MC.textRenderer;
		String text = setting.getName() + ":";
		fr.draw(matrixStack, text, x1, y1 + 2, 0xf0f0f0);
		
		renderIcon(matrixStack, stack, x3, y1, true);
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer tr = WurstClient.MC.textRenderer;
		String text = setting.getName() + ":";
		return tr.getWidth(text) + BLOCK_WITDH + 4;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return BLOCK_WITDH;
	}
	
	private void renderIcon(MatrixStack matrixStack, ItemStack stack, int x,
		int y, boolean large)
	{
		matrixStack.push();
		
		matrixStack.translate(x, y, 0);
		float scale = large ? 1.5F : 0.75F;
		// matrixStack.scale(scale, scale, scale);
		matrixStack.scale(scale, scale, scale);
		
		// RenderSystem.enableTexture();
		// RenderSystem.setShader(GameRenderer::method_34542);
		DiffuseLighting.enableGuiDepthLighting();
		ItemStack grass = new ItemStack(Blocks.GRASS_BLOCK);
		ItemStack renderStack = !stack.isEmpty() ? stack : grass;
		WurstClient.MC.getItemRenderer().renderInGuiWithOverrides(renderStack,
			0, 0);
		DiffuseLighting.disableGuiDepthLighting();
		
		matrixStack.pop();
		
		if(stack.isEmpty())
			renderQuestionMark(matrixStack, x, y, large);
	}
	
	private void renderQuestionMark(MatrixStack matrixStack, int x, int y,
		boolean large)
	{
		matrixStack.push();
		
		matrixStack.translate(x, y, 0);
		if(large)
			matrixStack.scale(2, 2, 2);
		
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		TextRenderer tr = WurstClient.MC.textRenderer;
		tr.drawWithShadow(matrixStack, "?", 3, 2, 0xf0f0f0);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		
		matrixStack.pop();
	}
	
	private String getBlockName(ItemStack stack)
	{
		if(stack.isEmpty())
			return "\u00a7ounknown block\u00a7r";
		else
			return stack.getName().getString();
	}
}
