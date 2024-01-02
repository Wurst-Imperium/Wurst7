/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.client.option.SimpleOption;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.events.MouseScrollListener;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.MathUtils;

@SearchTags({"telescope", "optifine"})
@DontBlock
public final class ZoomOtf extends OtherFeature implements MouseScrollListener
{
	private final SliderSetting level = new SliderSetting("Zoom level", 3, 1,
		50, 0.1, ValueDisplay.DECIMAL.withSuffix("x"));
	
	private final CheckboxSetting scroll = new CheckboxSetting(
		"Use mouse wheel",
		"If enabled, you can use the mouse wheel while zooming to zoom in even further.",
		true);
	
	private Double currentLevel;
	private Double defaultMouseSensitivity;
	
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
		SimpleOption<Double> mouseSensitivitySetting =
			MC.options.getMouseSensitivity();
		
		if(currentLevel == null)
			currentLevel = level.getValue();
		
		if(!WURST.getZoomKey().isPressed())
		{
			currentLevel = level.getValue();
			
			if(defaultMouseSensitivity != null)
			{
				mouseSensitivitySetting.setValue(defaultMouseSensitivity);
				defaultMouseSensitivity = null;
			}
			
			return fov;
		}
		
		if(defaultMouseSensitivity == null)
			defaultMouseSensitivity = mouseSensitivitySetting.getValue();
			
		// Adjust mouse sensitivity in relation to zoom level.
		// 1.0 / currentLevel is a value between 0.02 (50x zoom)
		// and 1 (no zoom).
		mouseSensitivitySetting
			.setValue(defaultMouseSensitivity * (1.0 / currentLevel));
		
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
	
	public boolean shouldPreventHotbarScrolling()
	{
		return WURST.getZoomKey().isPressed() && scroll.isChecked();
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
