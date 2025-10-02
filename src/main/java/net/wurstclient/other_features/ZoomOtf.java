/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.events.MouseScrollListener;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.TextFieldSetting;
import net.wurstclient.util.MathUtils;

@SearchTags({"telescope", "optifine"})
@DontBlock
public final class ZoomOtf extends OtherFeature implements MouseScrollListener
{
	private final SliderSetting level = new SliderSetting("Zoom level", 3, 1,
		50, 0.1, ValueDisplay.DECIMAL.withSuffix("x"));
	
	private final CheckboxSetting scroll = new CheckboxSetting(
		"Use mouse wheel", "If enabled, you can use the mouse wheel while"
			+ " zooming to zoom in even further.",
		true);
	
	private final CheckboxSetting zoomInScreens = new CheckboxSetting(
		"Zoom in screens", "If enabled, you can also zoom while a screen (chat,"
			+ " inventory, etc.) is open.",
		false);
	
	private final TextFieldSetting keybind = new TextFieldSetting("Keybind",
		"Determines the zoom keybind.\n\n"
			+ "Instead of editing this value manually, you should go to Wurst"
			+ " Options -> Zoom and set it there.",
		"key.keyboard.v", this::isValidKeybind);
	
	private Double currentLevel;
	private Double defaultMouseSensitivity;
	
	public ZoomOtf()
	{
		super("Zoom", "Allows you to zoom in.\n"
			+ "By default, the zoom is activated by pressing the \u00a7lV\u00a7r key.\n"
			+ "Go to Wurst Options -> Zoom to change this keybind.");
		addSetting(level);
		addSetting(scroll);
		addSetting(zoomInScreens);
		addSetting(keybind);
		EVENTS.add(MouseScrollListener.class, this);
	}
	
	public float changeFovBasedOnZoom(float fov)
	{
		SimpleOption<Double> mouseSensitivitySetting =
			MC.options.getMouseSensitivity();
		
		if(currentLevel == null)
			currentLevel = level.getValue();
		
		if(!isZoomKeyPressed())
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
		
		return (float)(fov / currentLevel);
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!isZoomKeyPressed() || !scroll.isChecked())
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
		return isZoomKeyPressed() && scroll.isChecked();
	}
	
	public Text getTranslatedKeybindName()
	{
		return InputUtil.fromTranslationKey(keybind.getValue())
			.getLocalizedText();
	}
	
	public void setBoundKey(String translationKey)
	{
		keybind.setValue(translationKey);
	}
	
	private boolean isZoomKeyPressed()
	{
		if(MC.currentScreen != null && !zoomInScreens.isChecked())
			return false;
		
		return InputUtil.isKeyPressed(MC.getWindow(),
			InputUtil.fromTranslationKey(keybind.getValue()).getCode());
	}
	
	private boolean isValidKeybind(String keybind)
	{
		try
		{
			return InputUtil.fromTranslationKey(keybind) != null;
			
		}catch(IllegalArgumentException e)
		{
			return false;
		}
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
