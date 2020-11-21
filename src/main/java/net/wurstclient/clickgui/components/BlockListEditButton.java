/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.screens.EditBlockListScreen;
import net.wurstclient.settings.BlockListSetting;

public final class BlockListEditButton extends Component
{
	private final BlockListSetting setting;
	private int buttonWidth;
	
	public BlockListEditButton(BlockListSetting setting)
	{
		this.setting = setting;
		
		TextRenderer fr = WurstClient.MC.textRenderer;
		buttonWidth = fr.getWidth("Edit...");
		
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		if(mouseX < getX() + getWidth() - buttonWidth - 4)
			return;
		
		WurstClient.MC.openScreen(
			new EditBlockListScreen(WurstClient.MC.currentScreen, setting));
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
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
		
		// tooltip
		if(hText)
			gui.setTooltip(setting.getDescription());
		
		// background
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x3, y1);
		GL11.glEnd();
		
		// box
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
			hBox ? opacity * 1.5F : opacity);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		// setting name
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TextRenderer fr = WurstClient.MC.textRenderer;
		String text = setting.getName() + ": " + setting.getBlockNames().size();
		fr.draw(matrixStack, text, x1, y1 + 2, 0xf0f0f0);
		fr.draw(matrixStack, "Edit...", x3 + 2, y1 + 2, 0xf0f0f0);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
	}
	
	@Override
	public int getDefaultWidth()
	{
		TextRenderer fr = WurstClient.MC.textRenderer;
		String text = setting.getName() + ": " + setting.getBlockNames().size();
		return fr.getWidth(text) + buttonWidth + 6;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
