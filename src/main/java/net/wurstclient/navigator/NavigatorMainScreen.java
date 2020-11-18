/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
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
		TextRenderer tr = WurstClient.MC.textRenderer;
		searchBar =
			new TextFieldWidget(tr, 0, 32, 200, 20, new LiteralText(""));
		searchBar.setHasBorder(false);
		searchBar.setMaxLength(128);
		
		children.add(searchBar);
		setInitialFocus(searchBar);
		searchBar.setSelected(true);
		
		searchBar.x = middleX - 100;
		setContentHeight(navigatorDisplayList.size() / 3 * 20);
	}
	
	@Override
	protected void onKeyPress(int keyCode, int scanCode, int int_3)
	{
		if(keyCode == GLFW.GLFW_KEY_BACKSPACE)
			if(clickTimer == -1)
				WurstClient.MC.openScreen((Screen)null);
			
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
			WurstClient.MC.openScreen((Screen)null);
			return;
		}
		
		if(hoveredFeature == -1)
			return;
		
		// arrow click, shift click, wheel click
		if(button == 0 && (hasShiftDown() || hoveringArrow) || button == 2)
		{
			expand(hoveredFeature);
			return;
		}
		
		// left click
		if(button == 0)
		{
			leftClick(hoveredFeature);
			return;
		}
		
		// right click
		// if(button == 1)
		// {
		// Feature feature = navigatorDisplayList.get(hoveredFeature);
		// if(feature.getHelpPage().isEmpty())
		// return;
		// MiscUtils.openLink("https://www.wurstclient.net/wiki/"
		// + feature.getHelpPage() + "/");
		// WurstClient wurst = WurstClient.INSTANCE;
		// wurst.navigator.addPreference(feature.getName());
		// ConfigFiles.NAVIGATOR.save();
		// }
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
		searchBar.tick();
		
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
				WurstClient.MC.openScreen(
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
	protected void onRender(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		
		boolean clickTimerRunning = clickTimer != -1;
		tooltip = null;
		
		// search bar
		if(!clickTimerRunning)
		{
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			WurstClient.MC.textRenderer.draw(matrixStack, "Search: ",
				middleX - 150, 32, 0xffffff);
			searchBar.render(matrixStack, mouseX, mouseY, partialTicks);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
		}
		
		// feature list
		int listX = middleX - 154;
		if(!clickTimerRunning)
			hoveredFeature = -1;
		
		RenderUtils.scissorBox(0, 59, width, height - 42);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		
		for(int i = Math.max(-scroll * 3 / 20 - 3, 0); i < navigatorDisplayList
			.size(); i++)
		{
			int featureX = listX + 104 * (i % 3);
			int featureY = 60 + i / 3 * 20 + scroll;
			
			if(featureY < 40)
				continue;
			if(featureY > height - 40)
				break;
			
			renderFeature(matrixStack, mouseX, mouseY, partialTicks, i,
				featureX, featureY);
		}
		
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		
		// tooltip
		if(tooltip != null)
		{
			String[] lines = tooltip.split("\n");
			TextRenderer fr = client.textRenderer;
			
			int tw = 0;
			int th = lines.length * fr.fontHeight;
			for(String line : lines)
			{
				int lw = fr.getWidth(line);
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
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2], 0.75F);
			GL11.glBegin(GL11.GL_QUADS);
			GL11.glVertex2i(xt1, yt1);
			GL11.glVertex2i(xt1, yt2);
			GL11.glVertex2i(xt2, yt2);
			GL11.glVertex2i(xt2, yt1);
			GL11.glEnd();
			
			// outline
			GL11.glColor4f(acColor[0], acColor[1], acColor[2], 0.5F);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			GL11.glVertex2i(xt1, yt1);
			GL11.glVertex2i(xt1, yt2);
			GL11.glVertex2i(xt2, yt2);
			GL11.glVertex2i(xt2, yt1);
			GL11.glEnd();
			
			// text
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			for(int i = 0; i < lines.length; i++)
				fr.draw(matrixStack, lines[i], xt1 + 2,
					yt1 + 1 + i * fr.fontHeight, 0xffffff);
		}
	}
	
	private void renderFeature(MatrixStack matrixStack, int mouseX, int mouseY,
		float partialTicks, int i, int x, int y)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float opacity = gui.getOpacity();
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
			
			drawBackgroundBox(area.x, area.y, area.x + area.width,
				area.y + area.height);
			return;
		}
		
		// color
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
		
		if(feature.isEnabled())
			// if(feature.isBlocked())
			// GL11.glColor4f(1, 0, 0,
			// hovering ? opacity * 1.5F : opacity);
			// else
			GL11.glColor4f(0, 1, 0, renderAsHovered ? opacity * 1.5F : opacity);
		else
			GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
				renderAsHovered ? opacity * 1.5F : opacity);
		
		// tooltip
		String tt = feature.getDescription();
		// if(feature.isBlocked())
		// {
		// if(tt == null)
		// tt = "";
		// else
		// tt += "\n\n";
		// tt +=
		// "Your current YesCheat+ profile is blocking this feature.";
		// }
		if(hovering)
			tooltip = tt;
		
		// box & shadow
		drawBox(area.x, area.y, area.x + area.width, area.y + area.height);
		
		// separator
		int bx1 = area.x + area.width - area.height;
		int by1 = area.y + 2;
		int by2 = by1 + area.height - 4;
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2i(bx1, by1);
		GL11.glVertex2i(bx1, by2);
		GL11.glEnd();
		
		// hovering
		if(hovering)
			hoveringArrow = mouseX >= bx1;
		
		// arrow positions
		double oneThrird = area.height / 3D;
		double twoThrirds = area.height * 2D / 3D;
		double ax1 = bx1 + oneThrird - 2D;
		double ax2 = bx1 + twoThrirds + 2D;
		double ax3 = bx1 + area.height / 2D;
		double ay1 = area.y + oneThrird;
		double ay2 = area.y + twoThrirds;
		
		// arrow
		GL11.glColor4f(0, hovering ? 1 : 0.85F, 0, 1);
		GL11.glBegin(GL11.GL_TRIANGLES);
		GL11.glVertex2d(ax1, ay1);
		GL11.glVertex2d(ax2, ay1);
		GL11.glVertex2d(ax3, ay2);
		GL11.glEnd();
		
		// arrow shadow
		GL11.glLineWidth(1);
		GL11.glColor4f(0.0625F, 0.0625F, 0.0625F, 0.5F);
		GL11.glBegin(GL11.GL_LINE_LOOP);
		GL11.glVertex2d(ax1, ay1);
		GL11.glVertex2d(ax2, ay1);
		GL11.glVertex2d(ax3, ay2);
		GL11.glEnd();
		
		// text
		if(!clickTimerRunning)
		{
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			String buttonText = feature.getName();
			client.textRenderer.draw(matrixStack, buttonText, area.x + 4,
				area.y + 4, 0xffffff);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
		}
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
