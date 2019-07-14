/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.CheckboxComponent;
import net.wurstclient.clickgui.Component;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonUtils;

public class CheckboxSetting extends Setting implements CheckboxLock
{
	private boolean checked;
	private final boolean checkedByDefault;
	private CheckboxLock lock;
	private int navigatorPosY;
	
	public CheckboxSetting(String name, String description, boolean checked)
	{
		super(name, description);
		this.checked = checked;
		checkedByDefault = checked;
	}
	
	public CheckboxSetting(String name, boolean checked)
	{
		this(name, "", checked);
	}
	
	@Override
	public final boolean isChecked()
	{
		return isLocked() ? lock.isChecked() : checked;
	}
	
	public final boolean isCheckedByDefault()
	{
		return checkedByDefault;
	}
	
	public final void setChecked(boolean checked)
	{
		if(isLocked())
			return;
		
		setCheckedIgnoreLock(checked);
	}
	
	private void setCheckedIgnoreLock(boolean checked)
	{
		this.checked = checked;
		update();
		
		WurstClient.INSTANCE.saveSettings();
	}
	
	public final boolean isLocked()
	{
		return lock != null;
	}
	
	public final void lock(CheckboxLock lock)
	{
		this.lock = lock;
		update();
	}
	
	public final void unlock()
	{
		lock = null;
		update();
	}
	
	@Override
	public final Component getComponent()
	{
		return new CheckboxComponent(this);
	}
	
	@Override
	public final void fromJson(JsonElement json)
	{
		if(!JsonUtils.isBoolean(json))
			return;
		
		setCheckedIgnoreLock(json.getAsBoolean());
	}
	
	@Override
	public final JsonElement toJson()
	{
		return new JsonPrimitive(checked);
	}
	
	// @Override
	// public final void addToFeatureScreen(NavigatorFeatureScreen
	// featureScreen)
	// {
	// navigatorPosY = 60 + featureScreen.getTextHeight() + 4;
	//
	// featureScreen.addText("\n\n");
	// featureScreen.addCheckbox(this);
	// }
	
	public final int getNavigatorPosY()
	{
		return navigatorPosY;
	}
	
	@Override
	public final ArrayList<PossibleKeybind> getPossibleKeybinds(
		String featureName)
	{
		String fullName = featureName + " " + getName();
		
		String command = ".setcheckbox " + featureName.toLowerCase() + " ";
		command += getName().toLowerCase().replace(" ", "_") + " ";
		
		ArrayList<PossibleKeybind> pkb = new ArrayList<>();
		pkb.add(new PossibleKeybind(command + "toggle", "Toggle " + fullName));
		pkb.add(new PossibleKeybind(command + "on", "Check " + fullName));
		pkb.add(new PossibleKeybind(command + "off", "Uncheck " + fullName));
		
		return pkb;
	}
}
