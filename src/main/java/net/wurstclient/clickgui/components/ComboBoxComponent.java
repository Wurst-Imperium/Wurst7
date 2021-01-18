/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
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
	public void render(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - 11;
		int x4 = x3 - popupWidth - 4;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, x2, y1, y2);
		boolean hText = hovering && mouseX < x4;
		boolean hBox = hovering && mouseX >= x4;
		
		// tooltip
		if(hText)
			gui.setTooltip(setting.getDescription());
		
		drawBackground(x1, x4, y1, y2);
		drawBox(x2, x4, y1, y2, hBox);
		
		drawSeparator(x3, y1, y2);
		drawArrow(x2, x3, y1, y2, hBox);
		
		drawNameAndValue(matrixStack, x1, x4, y1);
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
	
	private void drawBackground(int x1, int x4, int y1, int y2)
	{
		float[] bgColor = gui.getBgColor();
		float opacity = gui.getOpacity();
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], opacity);
		
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x4, y2);
		GL11.glVertex2i(x4, y1);
		GL11.glEnd();
	}
	
	private void drawBox(int x2, int x4, int y1, int y2, boolean hBox)
	{
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
		// background
		float bgAlpha = hBox ? opacity * 1.5F : opacity;
		GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], bgAlpha);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex2i(x4, y1);
		GL11.glVertex2i(x4, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2i(x4, y1);
		GL11.glVertex2i(x4, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
		GL11.glEnd();
	}
	
	private void drawSeparator(int x3, int y1, int y2)
	{
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2i(x3, y1);
		GL11.glVertex2i(x3, y2);
		GL11.glEnd();
	}
	
	private void drawArrow(int x2, int x3, int y1, int y2, boolean hBox)
	{
		double xa1 = x3 + 1;
		double xa2 = (x3 + x2) / 2.0;
		double xa3 = x2 - 1;
		double ya1;
		double ya2;
		
		if(isPopupOpen())
		{
			ya1 = y2 - 3.5;
			ya2 = y1 + 3;
			GL11.glColor4f(hBox ? 1 : 0.85F, 0, 0, 1);
			
		}else
		{
			ya1 = y1 + 3.5;
			ya2 = y2 - 3;
			GL11.glColor4f(0, hBox ? 1 : 0.85F, 0, 1);
		}
		
		// arrow
		GL11.glBegin(GL11.GL_TRIANGLES);
		GL11.glVertex2d(xa1, ya1);
		GL11.glVertex2d(xa3, ya1);
		GL11.glVertex2d(xa2, ya2);
		GL11.glEnd();
		
		// outline
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(xa1, ya1);
		GL11.glVertex2d(xa3, ya1);
		GL11.glVertex2d(xa2, ya2);
		GL11.glEnd();
	}
	
	private void drawNameAndValue(MatrixStack matrixStack, int x1, int x4,
		int y1)
	{
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		
		String name = setting.getName();
		String value = "" + setting.getSelected();
		int color = 0xF0F0F0;
		
		tr.draw(matrixStack, name, x1, y1 + 2, color);
		tr.draw(matrixStack, value, x4 + 2, y1 + 2, color);
		
		GL11.glDisable(GL11.GL_TEXTURE_2D);
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
