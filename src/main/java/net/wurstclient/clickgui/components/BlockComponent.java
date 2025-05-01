/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.lwjgl.glfw.GLFW;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.Window;
import net.wurstclient.clickgui.screens.EditBlockScreen;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.util.RenderUtils;

public final class BlockComponent extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
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
		
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			MC.setScreen(new EditBlockScreen(MC.currentScreen, setting));
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			setting.resetToDefault();
			break;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x2 - BLOCK_WITDH;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY, x1, y1, x2, y2);
		boolean hText = hovering && mouseX < x3;
		boolean hBlock = hovering && mouseX >= x3;
		
		// tooltip
		if(hText)
			GUI.setTooltip(setting.getWrappedDescription(200));
		else if(hBlock)
			GUI.setTooltip(getBlockTooltip());
		
		// background
		int bgColor =
			RenderUtils.toIntColor(GUI.getBgColor(), GUI.getOpacity());
		context.fill(x1, y1, x2, y2, bgColor);
		
		context.state.goUpLayer();
		
		// text
		String name = setting.getName() + ":";
		context.drawText(TR, name, x1, y1 + 2, GUI.getTxtColor(), false);
		
		// block
		ItemStack stack = new ItemStack(setting.getBlock());
		RenderUtils.drawItem(context, stack, x3, y1, true);
		
		context.state.goDownLayer();
	}
	
	private boolean isHovering(int mouseX, int mouseY, int x1, int y1, int x2,
		int y2)
	{
		Window parent = getParent();
		boolean scrollEnabled = parent.isScrollingEnabled();
		int scroll = scrollEnabled ? parent.getScrollOffset() : 0;
		
		return mouseX >= x1 && mouseY >= y1 && mouseX < x2 && mouseY < y2
			&& mouseY >= -scroll && mouseY < parent.getHeight() - 13 - scroll;
	}
	
	private String getBlockTooltip()
	{
		Block block = setting.getBlock();
		BlockState state = block.getDefaultState();
		ItemStack stack = new ItemStack(block);
		
		String translatedName = stack.isEmpty() ? "\u00a7ounknown block\u00a7r"
			: stack.getName().getString();
		String tooltip = "\u00a76Name:\u00a7r " + translatedName;
		
		String blockId = setting.getBlockName();
		tooltip += "\n\u00a76ID:\u00a7r " + blockId;
		
		int blockNumber = Block.getRawIdFromState(state);
		tooltip += "\n\u00a76Block #:\u00a7r " + blockNumber;
		
		tooltip += "\n\n\u00a7e[left-click]\u00a7r to edit";
		tooltip += "\n\u00a7e[right-click]\u00a7r to reset";
		
		return tooltip;
	}
	
	@Override
	public int getDefaultWidth()
	{
		return TR.getWidth(setting.getName() + ":") + BLOCK_WITDH + 4;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return BLOCK_WITDH;
	}
}
