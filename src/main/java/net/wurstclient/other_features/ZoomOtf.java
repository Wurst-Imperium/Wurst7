/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.network.chat.Component;
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
	private final SliderSetting zoomLevel = new SliderSetting("Zoom level", 3,
		1, 50, 0.1, ValueDisplay.DECIMAL.withSuffix("x"));
	
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
	
	public ZoomOtf()
	{
		super("Zoom", "Allows you to zoom in.\n"
			+ "By default, the zoom is activated by pressing the \u00a7lV\u00a7r key.\n"
			+ "Go to Wurst Options -> Zoom to change this keybind.");
		addSetting(zoomLevel);
		addSetting(scroll);
		addSetting(zoomInScreens);
		addSetting(keybind);
		EVENTS.add(MouseScrollListener.class, this);
	}
	
	public float changeFovBasedOnZoom(float fov)
	{
		return (float)(fov / getZoomFactor());
	}
	
	public double changeMouseSensitivityBasedOnZoom(double sensitivity)
	{
		return sensitivity * (1.0 / getZoomFactor());
	}
	
	private double getZoomFactor()
	{
		if(!isZoomKeyPressed())
		{
			currentLevel = zoomLevel.getValue();
			return 1;
		}
		
		if(currentLevel == null)
			currentLevel = zoomLevel.getValue();
		
		return currentLevel;
	}
	
	@Override
	public void onMouseScroll(double amount)
	{
		if(!isZoomKeyPressed() || !scroll.isChecked())
			return;
		
		if(currentLevel == null)
			currentLevel = zoomLevel.getValue();
		
		if(amount > 0)
			currentLevel *= 1.1;
		else if(amount < 0)
			currentLevel *= 0.9;
		
		currentLevel = MathUtils.clamp(currentLevel, zoomLevel.getMinimum(),
			zoomLevel.getMaximum());
	}
	
	public boolean isControllingScrollEvents()
	{
		return isZoomKeyPressed() && scroll.isChecked();
	}
	
	public Component getTranslatedKeybindName()
	{
		return InputConstants.getKey(keybind.getValue()).getDisplayName();
	}
	
	public void setBoundKey(String translationKey)
	{
		keybind.setValue(translationKey);
	}
	
	private boolean isZoomKeyPressed()
	{
		if(MC.gui.screen() != null && !zoomInScreens.isChecked())
			return false;
		
		return InputConstants.isKeyDown(MC.getWindow(),
			InputConstants.getKey(keybind.getValue()).getValue());
	}
	
	private boolean isValidKeybind(String keybind)
	{
		try
		{
			return InputConstants.getKey(keybind) != null;
			
		}catch(IllegalArgumentException e)
		{
			return false;
		}
	}
	
	public SliderSetting getLevelSetting()
	{
		return zoomLevel;
	}
	
	public CheckboxSetting getScrollSetting()
	{
		return scroll;
	}
}
