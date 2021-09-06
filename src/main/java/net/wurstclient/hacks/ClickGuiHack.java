/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.screens.ClickGuiScreen;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@DontSaveState
@DontBlock
@SearchTags({"click gui", "WindowGUI", "window gui", "HackMenu", "hack menu"})
public final class ClickGuiHack extends Hack
{
	private final SliderSetting opacity = new SliderSetting("Opacity", 0.5,
		0.15, 0.85, 0.01, ValueDisplay.PERCENTAGE);
	
	private final ColorSetting bgColor =
		new ColorSetting("BG", "Background color", new Color(64, 64, 64));
	
	private final ColorSetting acColor =
		new ColorSetting("AC", "Accent color", new Color(16, 16, 16));
	
	public ClickGuiHack()
	{
		super("ClickGUI", "Window-based ClickGUI.");
		addSetting(opacity);
		addSetting(bgColor);
		addSetting(acColor);
	}
	
	@Override
	public void onEnable()
	{
		MC.setScreen(new ClickGuiScreen(WURST.getGui()));
		setEnabled(false);
	}
	
	public float getOpacity()
	{
		return opacity.getValueF();
	}
	
	public float[] getBgColor()
	{
		return bgColor.getColorF();
	}
	
	public float[] getAcColor()
	{
		return acColor.getColorF();
	}
}
