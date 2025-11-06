/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.ClickGuiIcons;
import net.wurstclient.clickgui.Component;
import net.wurstclient.settings.PlantTypeSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.text.WText;

public final class PlantTypeComponent extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	private static final int BOX_SIZE = 11;
	private static final int ICON_SIZE = 24;
	private static final String HARVEST = "Harvest";
	private static final String REPLANT = "Replant";
	
	private final PlantTypeSetting setting;
	
	public PlantTypeComponent(PlantTypeSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton,
		Click context)
	{
		if(mouseX < getX() + ICON_SIZE)
			return;
		
		if(mouseY < getY() + getHeight() - BOX_SIZE)
			return;
		
		boolean hHarvest =
			mouseX < getX() + ICON_SIZE + BOX_SIZE + TR.getWidth(HARVEST) + 4;
		
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			if(hHarvest)
				setting.toggleHarvestingEnabled();
			else
				setting.toggleReplantingEnabled();
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			if(hHarvest)
				setting.resetHarvestingEnabled();
			else
				setting.resetReplantingEnabled();
			break;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int harvestWidth = TR.getWidth(HARVEST);
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + ICON_SIZE;
		int x4 = x3 + BOX_SIZE;
		int x5 = x4 + harvestWidth + 4;
		int x6 = x5 + BOX_SIZE;
		int y1 = getY();
		int y2 = y1 + getHeight();
		int y3 = y2 - BOX_SIZE;
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hIcon = hovering && mouseX < x3;
		boolean hName = hovering && mouseX >= x3 && mouseY < y3;
		boolean hHarvest =
			hovering && mouseX >= x3 && mouseX < x5 && mouseY >= y3;
		boolean hReplant = hovering && mouseX >= x5 && mouseY >= y3;
		
		if(hIcon)
			GUI.setTooltip(setting.getIcon().getName().getString());
		else if(hName)
			GUI.setTooltip(setting.getWrappedDescription(200));
		else if(hHarvest)
			GUI.setTooltip("" + WText.translated("gui.wurst.autofarm.harvest"));
		else if(hReplant)
			GUI.setTooltip("" + WText.translated("gui.wurst.autofarm.replant"));
		
		// background
		int bgColor = getFillColor(false);
		context.fill(x1, y1, x2, y3, bgColor);
		context.fill(x1, y3, x3, y2, bgColor);
		context.fill(x4, y3, x5, y2, bgColor);
		context.fill(x6, y3, x2, y2, bgColor);
		
		// icon
		RenderUtils.drawItem(context, setting.getIcon(), x1, y1, true);
		
		// boxes
		context.fill(x3, y3, x4, y2, getFillColor(hHarvest));
		context.fill(x5, y3, x6, y2, getFillColor(hReplant));
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x3, y3, x4, y2, outlineColor);
		RenderUtils.drawBorder2D(context, x5, y3, x6, y2, outlineColor);
		
		// checks
		if(setting.isHarvestingEnabled())
			ClickGuiIcons.drawCheck(context, x3, y3, x4, y2, hHarvest, false);
		if(setting.isReplantingEnabled())
			ClickGuiIcons.drawCheck(context, x5, y3, x6, y2, hReplant, false);
		
		// text
		String name = setting.getName();
		context.drawText(TR, name, x3 + 2, y1 + 3, GUI.getTxtColor(), false);
		context.drawText(TR, HARVEST, x4 + 2, y3 + 2, GUI.getTxtColor(), false);
		context.drawText(TR, REPLANT, x6 + 2, y3 + 2, GUI.getTxtColor(), false);
	}
	
	private int getFillColor(boolean hovering)
	{
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(GUI.getBgColor(), opacity);
	}
	
	@Override
	public int getDefaultWidth()
	{
		int nameWidth = TR.getWidth(setting.getName());
		int boxesWidth =
			2 * BOX_SIZE + TR.getWidth(HARVEST) + TR.getWidth(REPLANT) + 6;
		return ICON_SIZE + Math.max(nameWidth, boxesWidth);
	}
	
	@Override
	public int getDefaultHeight()
	{
		return ICON_SIZE;
	}
}
