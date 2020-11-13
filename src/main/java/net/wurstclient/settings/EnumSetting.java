/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.LinkedHashSet;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.ComboBoxComponent;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonUtils;

public final class EnumSetting<T extends Enum<T>> extends Setting
{
	private final T[] values;
	private T selected;
	private final T defaultSelected;
	
	public EnumSetting(String name, String description, T[] values, T selected)
	{
		super(name, description);
		this.values = Objects.requireNonNull(values);
		this.selected = Objects.requireNonNull(selected);
		defaultSelected = selected;
	}
	
	public EnumSetting(String name, T[] values, T selected)
	{
		this(name, "", values, selected);
	}
	
	public T[] getValues()
	{
		return values;
	}
	
	public T getSelected()
	{
		return selected;
	}
	
	public T getDefaultSelected()
	{
		return defaultSelected;
	}
	
	public void setSelected(T selected)
	{
		this.selected = Objects.requireNonNull(selected);
		WurstClient.INSTANCE.saveSettings();
	}
	
	public boolean setSelected(String selected)
	{
		for(T value : values)
		{
			if(!value.toString().equalsIgnoreCase(selected))
				continue;
			
			setSelected(value);
			return true;
		}
		
		return false;
	}
	
	public void selectNext()
	{
		int next = selected.ordinal() + 1;
		if(next >= values.length)
			next = 0;
		
		setSelected(values[next]);
	}
	
	public void selectPrev()
	{
		int prev = selected.ordinal() - 1;
		if(prev < 0)
			prev = values.length - 1;
		
		setSelected(values[prev]);
	}
	
	@Override
	public Component getComponent()
	{
		return new ComboBoxComponent<>(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		if(!JsonUtils.isString(json))
			return;
		
		setSelected(json.getAsString());
	}
	
	@Override
	public JsonElement toJson()
	{
		return new JsonPrimitive(selected.toString());
	}
	
	@Override
	public LinkedHashSet<PossibleKeybind> getPossibleKeybinds(
		String featureName)
	{
		String fullName = featureName + " " + getName();
		
		String command = ".setmode " + featureName.toLowerCase() + " "
			+ getName().toLowerCase().replace(" ", "_") + " ";
		String description = "Set " + fullName + " to ";
		
		LinkedHashSet<PossibleKeybind> pkb = new LinkedHashSet<>();
		pkb.add(new PossibleKeybind(command + "next", "Next " + fullName));
		pkb.add(new PossibleKeybind(command + "prev", "Previous " + fullName));
		
		for(T v : values)
		{
			String vName = v.toString().replace(" ", "_").toLowerCase();
			pkb.add(new PossibleKeybind(command + vName, description + v));
		}
		
		return pkb;
	}
}
