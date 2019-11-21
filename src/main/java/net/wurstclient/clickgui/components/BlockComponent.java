/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.Blocks;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.util.TextFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
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
	public void render(int mouseX, int mouseY, float partialTicks)
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
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		// setting name
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TextRenderer fr = WurstClient.MC.textRenderer;
		String text = setting.getName() + ":";
		fr.draw(text, x1, y1 + 2, 0xf0f0f0);
		
		renderIcon(stack, x3, y1, true);
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer tr = WurstClient.MC.textRenderer;
		String text = setting.getName() + ":";
		return tr.getStringWidth(text) + BLOCK_WITDH + 4;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return BLOCK_WITDH;
	}
	
	private void renderIcon(ItemStack stack, int x, int y, boolean large)
	{
		GL11.glPushMatrix();
		
		GL11.glTranslated(x, y, 0);
		double scale = large ? 1.5 : 0.75;
		GL11.glScaled(scale, scale, scale);
		
		MatrixStack matrixStack = new MatrixStack();
		GuiLighting.enableForItems(matrixStack.peek().getModel());
		ItemStack grass = new ItemStack(Blocks.GRASS_BLOCK);
		ItemStack renderStack = !stack.isEmpty() ? stack : grass;
		WurstClient.MC.getItemRenderer().renderGuiItem(renderStack, 0, 0);
		GuiLighting.disable();
		
		GL11.glPopMatrix();
		
		if(stack.isEmpty())
			renderQuestionMark(x, y, large);
	}
	
	private void renderQuestionMark(int x, int y, boolean large)
	{
		GL11.glPushMatrix();
		
		GL11.glTranslated(x, y, 0);
		if(large)
			GL11.glScaled(2, 2, 2);
		
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		TextRenderer tr = WurstClient.MC.textRenderer;
		tr.drawWithShadow("?", 3, 2, 0xf0f0f0);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		
		GL11.glPopMatrix();
	}
	
	private String getBlockName(ItemStack stack)
	{
		if(stack.isEmpty())
			return TextFormat.ITALIC + "unknown block" + TextFormat.RESET;
		else
			return stack.getName().asFormattedString();
	}
}
