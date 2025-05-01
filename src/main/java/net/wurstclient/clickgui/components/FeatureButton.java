/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Objects;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.wurstclient.Feature;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.ClickGuiIcons;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.SettingsWindow;
import net.wurstclient.clickgui.Window;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

public final class FeatureButton extends Component
{
	private static final ClickGui GUI = WURST.getGui();
	private static final TextRenderer TR = MC.textRenderer;
	
	private final Feature feature;
	private final boolean hasSettings;
	
	private Window settingsWindow;
	
	public FeatureButton(Feature feature)
	{
		this.feature = Objects.requireNonNull(feature);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
		hasSettings = !feature.getSettings().isEmpty();
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton)
	{
		if(mouseButton != 0)
			return;
		
		if(hasSettings && (mouseX > getX() + getWidth() - 12
			|| feature.getPrimaryAction().isEmpty()))
		{
			toggleSettingsWindow();
			return;
		}
		
		TooManyHaxHack tooManyHax = WURST.getHax().tooManyHaxHack;
		if(tooManyHax.isEnabled() && tooManyHax.isBlocked(feature))
		{
			ChatUtils.error(feature.getName() + " is blocked by TooManyHax.");
			return;
		}
		
		feature.doPrimaryAction();
	}
	
	private boolean isSettingsWindowOpen()
	{
		return settingsWindow != null && !settingsWindow.isClosing();
	}
	
	private void toggleSettingsWindow()
	{
		if(!isSettingsWindowOpen())
		{
			settingsWindow = new SettingsWindow(feature, getParent(), getY());
			GUI.addWindow(settingsWindow);
			
		}else
		{
			settingsWindow.close();
			settingsWindow = null;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = hasSettings ? x2 - 11 : x2;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hFeature = hovering && mouseX < x3;
		boolean hSettings = hovering && mouseX >= x3;
		
		if(hFeature)
			GUI.setTooltip(feature.getWrappedDescription(200));
		
		// buttons
		context.fill(x1, y1, x3, y2,
			getButtonColor(feature.isEnabled(), hFeature));
		if(hasSettings)
			context.fill(x3, y1, x2, y2, getButtonColor(false, hSettings));
		
		context.state.goUpLayer();
		
		// outlines
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x1, y1, x2, y2, outlineColor);
		if(hasSettings)
			RenderUtils.drawLine2D(context, x3, y1, x3, y2, outlineColor);
		
		// arrow
		if(hasSettings)
			ClickGuiIcons.drawMinimizeArrow(context, x3, y1 + 0.5F, x2,
				y2 - 0.5F, hSettings, !isSettingsWindowOpen());
		
		// text
		String name = feature.getName();
		int tx = x1 + (x3 - x1 - TR.getWidth(name)) / 2;
		int ty = y1 + 2;
		context.drawText(TR, name, tx, ty, GUI.getTxtColor(), false);
		
		context.state.goDownLayer();
	}
	
	private int getButtonColor(boolean enabled, boolean hovering)
	{
		float[] rgb = enabled ? new float[]{0, 1, 0} : GUI.getBgColor();
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(rgb, opacity);
	}
	
	@Override
	public int getDefaultWidth()
	{
		int width = TR.getWidth(feature.getName());
		width += hasSettings ? 15 : 4;
		return width;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 11;
	}
}
