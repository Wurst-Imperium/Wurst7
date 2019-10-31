/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.wurstclient.SearchTags;
import net.wurstclient.events.MouseScrollListener;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.MathUtils;

@SearchTags({"telescope", "optifine"})
public final class ZoomOtf extends OtherFeature implements MouseScrollListener
{
	private final SliderSetting level = new SliderSetting("Zoom level", 3, 1,
		50, 0.1, v -> ValueDisplay.DECIMAL.getValueString(v) + "x");
	
	private final CheckboxSetting scroll = new CheckboxSetting(
		"Use mouse wheel", "If enabled, you can use the mouse wheel\n"
			+ "while zooming to zoom in even further.",
		true);
	
	private Double currentLevel;
	
	public ZoomOtf()
	{
		super("Zoom", "Allows you to zoom in.\n"
			+ "By default, the zoom is activated by pressing the \u00a7lV\u00a7r key.\n"
			+ "Go to Wurst Options -> Zoom to change this keybind.");
		addSetting(level);
		addSetting(scroll);
		EVENTS.add(MouseScrollListener.class, this);
	}
	
	public double changeFovBasedOnZoom(double fov)
	{
		if(currentLevel == null)
			currentLevel = level.getValue();
		
		if(!WURST.getZoomKey().isPressed())
		{
			currentLevel = level.getValue();
			return fov;
		}
		
		return fov / currentLevel;
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!WURST.getZoomKey().isPressed() || !scroll.isChecked())
			return;
		
		if(currentLevel == null)
			currentLevel = level.getValue();
		
		if(amount > 0)
			currentLevel *= 1.1;
		else if(amount < 0)
			currentLevel *= 0.9;
		
		currentLevel = MathUtils.clamp(currentLevel, level.getMinimum(),
			level.getMaximum());
	}
	
	public SliderSetting getLevelSetting()
	{
		return level;
	}
	
	public CheckboxSetting getScrollSetting()
	{
		return scroll;
	}
}
