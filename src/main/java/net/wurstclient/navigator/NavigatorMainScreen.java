/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.util.RenderUtils;

public final class NavigatorMainScreen extends NavigatorScreen
{
	private static final ArrayList<Feature> navigatorDisplayList =
		new ArrayList<>();
	private TextFieldWidget searchBar;
	private String tooltip;
	private int hoveredFeature = -1;
	private boolean hoveringArrow;
	private int clickTimer = -1;
	private boolean expanding = false;
	
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
		searchBar = new TextFieldWidget(tr, 0, 32, 200, 20, "");
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
		if(keyCode == 1)
			if(clickTimer == -1)
				WurstClient.MC.openScreen((Screen)null);
			
		if(clickTimer == -1)
		{
			String newText = searchBar.getText();
			Navigator navigator = WurstClient.INSTANCE.getNavigator();
			if(newText.isEmpty())
				navigator.copyNavigatorList(navigatorDisplayList);
			else
			{
				newText = newText.toLowerCase().trim();
				navigator.getSearchResults(navigatorDisplayList, newText);
			}
			setContentHeight(navigatorDisplayList.size() / 3 * 20);
		}
	}
	
	@Override
	protected void onMouseClick(double x, double y, int button)
	{
		if(clickTimer == -1 && hoveredFeature != -1)
			if(button == 0 && (hasShiftDown() || hoveringArrow) || button == 2)
				// arrow click, shift click, wheel click
				expanding = true;
			else if(button == 0)
			{
				// left click
				Feature feature = navigatorDisplayList.get(hoveredFeature);
				if(feature.getPrimaryAction().isEmpty())
					expanding = true;
				else
				{
					feature.doPrimaryAction();
					WurstClient wurst = WurstClient.INSTANCE;
					wurst.getNavigator().addPreference(feature.getName());
				}
			}else if(button == 1)
			{
				// right click
				// Feature feature = navigatorDisplayList.get(hoveredFeature);
				// if(feature.getHelpPage().isEmpty())
				// return;
				// MiscUtils.openLink("https://www.wurstclient.net/wiki/"
				// + feature.getHelpPage() + "/");
				// WurstClient wurst = WurstClient.INSTANCE;
				// wurst.navigator.addPreference(feature.getName());
				// ConfigFiles.NAVIGATOR.save();
			}
	}
	
	@Override
	protected void onUpdate()
	{
		searchBar.tick();
		
		if(expanding)
			if(clickTimer < 4)
				clickTimer++;
			else
			{
				Feature feature = navigatorDisplayList.get(hoveredFeature);
				WurstClient.MC
					.openScreen(new NavigatorFeatureScreen(feature, this));
			}
		else if(!expanding && clickTimer > -1)
			clickTimer--;
		scrollbarLocked = clickTimer != -1;
	}
	
	@Override
	protected void onRender(int mouseX, int mouseY, float partialTicks)
	{
		ClickGui gui = WurstClient.INSTANCE.getGui();
		float[] bgColor = gui.getBgColor();
		float[] acColor = gui.getAcColor();
		float opacity = gui.getOpacity();
		
		boolean clickTimerNotRunning = clickTimer == -1;
		tooltip = null;
		
		// search bar
		if(clickTimerNotRunning)
		{
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			WurstClient.MC.textRenderer.draw("Search: ", middleX - 150, 32,
				0xffffff);
			searchBar.render(mouseX, mouseY, partialTicks);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_BLEND);
		}
		
		// feature list
		int x = middleX - 50;
		if(clickTimerNotRunning)
			hoveredFeature = -1;
		RenderUtils.scissorBox(0, 59, width, height - 42);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		for(int i = Math.max(-scroll * 3 / 20 - 3, 0); i < navigatorDisplayList
			.size(); i++)
		{
			// y position
			int y = 60 + i / 3 * 20 + scroll;
			if(y < 40)
				continue;
			if(y > height - 40)
				break;
			
			// x position
			int xi = 0;
			switch(i % 3)
			{
				case 0:
				xi = x - 104;
				break;
				case 1:
				xi = x;
				break;
				case 2:
				xi = x + 104;
				break;
			}
			
			// feature & area
			Feature feature = navigatorDisplayList.get(i);
			Rectangle area = new Rectangle(xi, y, 100, 16);
			
			// click animation
			if(!clickTimerNotRunning)
			{
				if(i != hoveredFeature)
					continue;
				
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
			}else
			{
				// color
				boolean hovering = area.contains(mouseX, mouseY);
				if(hovering)
					hoveredFeature = i;
				if(feature.isEnabled())
					// if(feature.isBlocked())
					// GL11.glColor4f(1, 0, 0,
					// hovering ? opacity * 1.5F : opacity);
					// else
					GL11.glColor4f(0, 1, 0,
						hovering ? opacity * 1.5F : opacity);
				else
					GL11.glColor4f(bgColor[0], bgColor[1], bgColor[2],
						hovering ? opacity * 1.5F : opacity);
				
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
				drawBox(area.x, area.y, area.x + area.width,
					area.y + area.height);
				
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
				if(clickTimerNotRunning)
				{
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					String buttonText = feature.getName();
					minecraft.textRenderer.draw(buttonText, area.x + 4,
						area.y + 4, 0xffffff);
					GL11.glDisable(GL11.GL_TEXTURE_2D);
					GL11.glEnable(GL11.GL_BLEND);
				}
			}
		}
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
		
		// tooltip
		if(tooltip != null)
		{
			String[] lines = tooltip.split("\n");
			TextRenderer fr = minecraft.textRenderer;
			
			int tw = 0;
			int th = lines.length * fr.fontHeight;
			for(String line : lines)
			{
				int lw = fr.getStringWidth(line);
				if(lw > tw)
					tw = lw;
			}
			int sw = minecraft.currentScreen.width;
			int sh = minecraft.currentScreen.height;
			
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
				fr.draw(lines[i], xt1 + 2, yt1 + 1 + i * fr.fontHeight,
					0xffffff);
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
