/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.ClickGuiIcons;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

public final class NavigatorMainScreen extends NavigatorScreen
{
	private static final ArrayList<Feature> navigatorDisplayList =
		new ArrayList<>();
	private TextFieldWidget searchBar;
	private String lastSearchText = "";
	private String tooltip;
	private int hoveredFeature = -1;
	private int selectedFeature = 0;
	private boolean mouseMoved = false;
	private boolean hoveringArrow;
	private int clickTimer = -1;
	private boolean expanding = false;
	private Feature expandingFeature;
	
	public NavigatorMainScreen()
	{
		hasBackground = false;
		nonScrollableArea = 0;
		
		Navigator navigator = WurstClient.INSTANCE.getNavigator();
		navigator.copyNavigatorList(navigatorDisplayList);
	}
	
	@Override
	protected void onResize()
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		TextRenderer tr = WurstClient.MC.textRenderer;
		searchBar = new TextFieldWidget(tr, 0, 32, 200, 20, Text.literal(""));
		searchBar.setEditableColor(txtColor);
		searchBar.setDrawsBackground(false);
		searchBar.setMaxLength(128);
		
		addSelectableChild(searchBar);
		setFocused(searchBar);
		searchBar.setFocused(true);
		
		searchBar.setX(middleX - 100);
		setContentHeight(navigatorDisplayList.size() / 3 * 20);
	}
	
	@Override
	protected void onKeyPress(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_ENTER)
			leftClick(selectedFeature);
		
		if(keyCode == GLFW.GLFW_KEY_SPACE)
			expand(selectedFeature);
		
		if(keyCode == GLFW.GLFW_KEY_RIGHT
			|| keyCode == GLFW.GLFW_KEY_TAB && !hasShiftDown())
		{
			if(selectedFeature + 1 < navigatorDisplayList.size())
				selectedFeature++;
			
		}else if(keyCode == GLFW.GLFW_KEY_LEFT
			|| keyCode == GLFW.GLFW_KEY_TAB && hasShiftDown())
		{
			if(selectedFeature - 1 > -1)
				selectedFeature--;
			
		}else if(keyCode == GLFW.GLFW_KEY_DOWN)
		{
			if(selectedFeature + 3 < navigatorDisplayList.size())
				selectedFeature += 3;
			
		}else if(keyCode == GLFW.GLFW_KEY_UP)
			if(selectedFeature - 3 > -1)
				selectedFeature -= 3;
	}
	
	@Override
	protected void onMouseClick(double x, double y, int button)
	{
		if(clickTimer != -1)
			return;
		
		// back button
		if(button == GLFW.GLFW_MOUSE_BUTTON_4)
		{
			WurstClient.MC.setScreen((Screen)null);
			return;
		}
		
		if(hoveredFeature == -1)
			return;
		
		// arrow click, shift click, wheel click
		if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT
			&& (hasShiftDown() || hoveringArrow)
			|| button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
		{
			expand(hoveredFeature);
			return;
		}
		
		// left click
		if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
			leftClick(hoveredFeature);
	}
	
	private void expand(int i)
	{
		if(i < 0 || i >= navigatorDisplayList.size())
			return;
		
		expandingFeature = navigatorDisplayList.get(i);
		expanding = true;
	}
	
	private void leftClick(int i)
	{
		if(i < 0 || i >= navigatorDisplayList.size())
			return;
		
		Feature feature = navigatorDisplayList.get(i);
		
		if(feature.getPrimaryAction().isEmpty())
		{
			expanding = true;
			expandingFeature = feature;
			return;
		}
		
		WurstClient wurst = WurstClient.INSTANCE;
		TooManyHaxHack tooManyHax = wurst.getHax().tooManyHaxHack;
		
		if(tooManyHax.isEnabled() && tooManyHax.isBlocked(feature))
		{
			ChatUtils.error(feature.getName() + " is blocked by TooManyHax.");
			return;
		}
		
		feature.doPrimaryAction();
		wurst.getNavigator().addPreference(feature.getName());
	}
	
	@Override
	protected void onUpdate()
	{
		String newText = searchBar.getText();
		if(clickTimer == -1 && !newText.equals(lastSearchText))
		{
			Navigator navigator = WurstClient.INSTANCE.getNavigator();
			
			if(newText.isEmpty())
				navigator.copyNavigatorList(navigatorDisplayList);
			else
			{
				newText = newText.toLowerCase().trim();
				navigator.getSearchResults(navigatorDisplayList, newText);
			}
			
			setContentHeight(navigatorDisplayList.size() / 3 * 20);
			lastSearchText = newText;
			selectedFeature = 0;
		}
		
		if(expanding)
			if(clickTimer < 4)
				clickTimer++;
			else
				WurstClient.MC.setScreen(
					new NavigatorFeatureScreen(expandingFeature, this));
		else if(!expanding && clickTimer > -1)
			clickTimer--;
		
		scrollbarLocked = clickTimer != -1;
	}
	
	@Override
	public void mouseMoved(double mouseX, double mouseY)
	{
		mouseMoved = true;
	}
	
	@Override
	protected void onRender(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		int txtColor = gui.getTxtColor();
		
		boolean clickTimerRunning = clickTimer != -1;
		tooltip = null;
		
		// search bar
		if(!clickTimerRunning)
		{
			context.drawTextWithShadow(WurstClient.MC.textRenderer, "Search: ",
				middleX - 150, 32, txtColor);
			searchBar.render(context, mouseX, mouseY, partialTicks);
		}
		
		// feature list
		int listX = middleX - 154;
		if(!clickTimerRunning)
			hoveredFeature = -1;
		
		context.enableScissor(0, 59, width, height - 42);
		
		for(int i = Math.max(-scroll * 3 / 20 - 3, 0); i < navigatorDisplayList
			.size(); i++)
		{
			int featureX = listX + 104 * (i % 3);
			int featureY = 60 + i / 3 * 20 + scroll;
			
			if(featureY < 40)
				continue;
			if(featureY > height - 40)
				break;
			
			renderFeature(context, mouseX, mouseY, partialTicks, i, featureX,
				featureY);
		}
		
		context.disableScissor();
		
		// tooltip
		if(tooltip != null)
		{
			context.state.goUpLayer();
			
			String[] lines = tooltip.split("\n");
			TextRenderer tr = client.textRenderer;
			
			int tw = 0;
			int th = lines.length * tr.fontHeight;
			for(String line : lines)
			{
				int lw = tr.getWidth(line);
				if(lw > tw)
					tw = lw;
			}
			int sw = client.currentScreen.width;
			int sh = client.currentScreen.height;
			
			int xt1 = mouseX + tw + 11 <= sw ? mouseX + 8 : mouseX - tw - 8;
			int xt2 = xt1 + tw + 3;
			int yt1 = mouseY + th - 2 <= sh ? mouseY - 4 : mouseY - th - 4;
			int yt2 = yt1 + th + 2;
			
			// background
			int bgColor = RenderUtils.toIntColor(gui.getBgColor(),
				gui.getTooltipOpacity());
			context.fill(xt1, yt1, xt2, yt2, bgColor);
			
			// outline
			int acColor = RenderUtils.toIntColor(gui.getAcColor(), 0.5F);
			RenderUtils.drawBorder2D(context, xt1, yt1, xt2, yt2, acColor);
			
			// text
			context.state.goUpLayer();
			for(int i = 0; i < lines.length; i++)
				context.drawText(tr, lines[i], xt1 + 2,
					yt1 + 2 + i * tr.fontHeight, txtColor, false);
			context.state.goDownLayer();
			
			context.state.goDownLayer();
		}
	}
	
	private void renderFeature(DrawContext context, int mouseX, int mouseY,
		float partialTicks, int i, int x, int y)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		boolean clickTimerRunning = clickTimer != -1;
		
		Feature feature = navigatorDisplayList.get(i);
		Rectangle area = new Rectangle(x, y, 100, 16);
		
		// click animation
		if(clickTimerRunning)
		{
			if(feature != expandingFeature)
				return;
			
			float factor;
			if(expanding)
				if(clickTimer == 4)
					factor = 1F;
				else
					factor = (clickTimer + partialTicks) / 4F;
			else if(clickTimer == 0)
				factor = 0F;
			else
				factor = (clickTimer - partialTicks) / 4F;
			float antiFactor = 1 - factor;
			
			area.x = (int)(area.x * antiFactor + (middleX - 154) * factor);
			area.y = (int)(area.y * antiFactor + 60 * factor);
			area.width = (int)(area.width * antiFactor + 308 * factor);
			area.height =
				(int)(area.height * antiFactor + (height - 103) * factor);
			
			drawBackgroundBox(context, area.x, area.y, area.x + area.width,
				area.y + area.height);
			return;
		}
		
		boolean hovering = area.contains(mouseX, mouseY);
		if(hovering)
		{
			hoveredFeature = i;
			
			if(mouseMoved)
			{
				selectedFeature = i;
				mouseMoved = false;
			}
		}
		
		boolean renderAsHovered = hovering || selectedFeature == i;
		
		// tooltip
		if(hovering)
			tooltip = feature.getWrappedDescription(200);
		
		// box & shadow
		int featColor = RenderUtils.toIntColor(
			feature.isEnabled() ? new float[]{0, 1, 0} : gui.getBgColor(),
			gui.getOpacity() * (renderAsHovered ? 1.5F : 1));
		drawBox(context, area.x, area.y, area.x + area.width,
			area.y + area.height, featColor);
		
		// separator
		int bx1 = area.x + area.width - area.height;
		int by1 = area.y + 2;
		int by2 = by1 + area.height - 4;
		int sepColor = RenderUtils.toIntColor(gui.getAcColor(), 0.5F);
		RenderUtils.drawLine2D(context, bx1, by1, bx1, by2, sepColor);
		
		// hovering
		if(hovering)
			hoveringArrow = mouseX >= bx1;
		
		context.state.goUpLayer();
		
		// arrow
		ClickGuiIcons.drawMinimizeArrow(context, bx1 + 2, area.y + 2.5F,
			area.x + area.width - 2, area.y + area.height - 3, hovering, true);
		
		// text
		if(!clickTimerRunning)
		{
			TextRenderer tr = client.textRenderer;
			String buttonText = feature.getName();
			int bx = area.x + 4;
			int by = area.y + 4;
			int txtColor = gui.getTxtColor();
			context.drawText(tr, buttonText, bx, by, txtColor, false);
		}
		
		context.state.goDownLayer();
	}
	
	public void setExpanding(boolean expanding)
	{
		this.expanding = expanding;
	}
	
	@Override
	protected void onMouseDrag(double mouseX, double mouseY, int button,
		double double_3, double double_4)
	{
		
	}
	
	@Override
	protected void onMouseRelease(double x, double y, int button)
	{
		
	}
}
