/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.wurstclient.Category;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.events.KeyPressListener;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.other_features.TabGuiOtf;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

public final class TabGui implements KeyPressListener
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final Minecraft MC = WurstClient.MC;
	
	private final ArrayList<Tab> tabs = new ArrayList<>();
	private final TabGuiOtf tabGuiOtf =
		WurstClient.INSTANCE.getOtfs().tabGuiOtf;
	
	private int width;
	private int height;
	private int selected;
	private boolean tabOpened;
	
	public TabGui()
	{
		WURST.getEventManager().add(KeyPressListener.class, this);
		
		LinkedHashMap<Category, Tab> tabMap = new LinkedHashMap<>();
		for(Category category : Category.values())
			tabMap.put(category, new Tab(category.getName()));
		
		ArrayList<Feature> features = new ArrayList<>();
		features.addAll(WURST.getHax().getAllHax());
		features.addAll(WURST.getCmds().getAllCmds());
		features.addAll(WURST.getOtfs().getAllOtfs());
		
		for(Feature feature : features)
			if(feature.getCategory() != null)
				tabMap.get(feature.getCategory()).add(feature);
			
		tabs.addAll(tabMap.values());
		tabs.forEach(Tab::updateSize);
		updateSize();
	}
	
	private void updateSize()
	{
		width = 64;
		for(Tab tab : tabs)
		{
			int tabWidth = MC.font.width(tab.name) + 10;
			if(tabWidth > width)
				width = tabWidth;
		}
		height = tabs.size() * 10;
	}
	
	@Override
	public void onKeyPress(KeyPressEvent event)
	{
		if(event.getAction() != GLFW.GLFW_PRESS)
			return;
		
		if(tabGuiOtf.isHidden())
			return;
		
		if(tabOpened)
			switch(event.getKeyCode())
			{
				case GLFW.GLFW_KEY_LEFT:
				tabOpened = false;
				break;
				
				default:
				tabs.get(selected).onKeyPress(event.getKeyCode());
				break;
			}
		else
			switch(event.getKeyCode())
			{
				case GLFW.GLFW_KEY_DOWN:
				if(selected < tabs.size() - 1)
					selected++;
				else
					selected = 0;
				break;
				
				case GLFW.GLFW_KEY_UP:
				if(selected > 0)
					selected--;
				else
					selected = tabs.size() - 1;
				break;
				
				case GLFW.GLFW_KEY_RIGHT:
				tabOpened = true;
				break;
			}
	}
	
	public void render(GuiGraphics context, float partialTicks)
	{
		if(tabGuiOtf.isHidden())
			return;
		
		PoseStack matrixStack = context.pose();
		matrixStack.pushPose();
		matrixStack.translate(2, 23, 100);
		
		drawBox(context, 0, 0, width, height);
		context.enableScissor(2, 23, 2 + width, 23 + height);
		
		int textY = 1;
		int txtColor = WURST.getGui().getTxtColor();
		Font tr = MC.font;
		for(int i = 0; i < tabs.size(); i++)
		{
			String tabName = tabs.get(i).name;
			if(i == selected)
				tabName = (tabOpened ? "<" : ">") + tabName;
			
			context.drawString(tr, tabName, 2, textY, txtColor, false);
			textY += 10;
		}
		
		context.disableScissor();
		
		if(tabOpened)
		{
			Tab tab = tabs.get(selected);
			
			matrixStack.pushPose();
			matrixStack.translate(width + 2, 0, 0);
			
			drawBox(context, 0, 0, tab.width, tab.height);
			context.enableScissor(width + 4, 23, width + 4 + tab.width,
				23 + tab.height);
			
			int tabTextY = 1;
			for(int i = 0; i < tab.features.size(); i++)
			{
				Feature feature = tab.features.get(i);
				String fName = feature.getName();
				
				if(feature.isEnabled())
					fName = "\u00a7a" + fName + "\u00a7r";
				
				if(i == tab.selected)
					fName = ">" + fName;
				
				context.drawString(tr, fName, 2, tabTextY, txtColor, false);
				tabTextY += 10;
			}
			
			context.disableScissor();
			matrixStack.popPose();
		}
		
		matrixStack.popPose();
	}
	
	private void drawBox(GuiGraphics context, int x1, int y1, int x2, int y2)
	{
		ClickGui gui = WURST.getGui();
		int bgColor =
			RenderUtils.toIntColor(gui.getBgColor(), gui.getOpacity());
		
		context.fill(x1, y1, x2, y2, bgColor);
		RenderUtils.drawBoxShadow2D(context, x1, y1, x2, y2);
	}
	
	private static final class Tab
	{
		private final String name;
		private final ArrayList<Feature> features = new ArrayList<>();
		
		private int width;
		private int height;
		private int selected;
		
		public Tab(String name)
		{
			this.name = name;
		}
		
		public void updateSize()
		{
			width = 64;
			for(Feature feature : features)
			{
				int fWidth = MC.font.width(feature.getName()) + 10;
				if(fWidth > width)
					width = fWidth;
			}
			height = features.size() * 10;
		}
		
		public void onKeyPress(int keyCode)
		{
			switch(keyCode)
			{
				case GLFW.GLFW_KEY_DOWN:
				if(selected < features.size() - 1)
					selected++;
				else
					selected = 0;
				break;
				
				case GLFW.GLFW_KEY_UP:
				if(selected > 0)
					selected--;
				else
					selected = features.size() - 1;
				break;
				
				case GLFW.GLFW_KEY_ENTER:
				onEnter();
				break;
			}
		}
		
		private void onEnter()
		{
			Feature feature = features.get(selected);
			
			TooManyHaxHack tooManyHax = WURST.getHax().tooManyHaxHack;
			if(tooManyHax.isEnabled() && tooManyHax.isBlocked(feature))
			{
				ChatUtils
					.error(feature.getName() + " is blocked by TooManyHax.");
				return;
			}
			
			feature.doPrimaryAction();
		}
		
		public void add(Feature feature)
		{
			features.add(feature);
		}
	}
}
