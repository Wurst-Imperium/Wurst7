/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.TextFieldEditButton;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.text.WText;

public final class TextFieldSetting extends Setting
{
	private String value = "";
	private final String defaultValue;
	private final Predicate<String> validator;
	
	public TextFieldSetting(String name, WText description, String defaultValue,
		Predicate<String> validator)
	{
		super(name, description);
		
		Objects.requireNonNull(defaultValue);
		Objects.requireNonNull(validator);
		if(!validator.test(defaultValue))
			throw new IllegalArgumentException(
				"Default value is not valid: " + defaultValue);
		
		value = defaultValue;
		this.defaultValue = defaultValue;
		this.validator = validator;
	}
	
	public TextFieldSetting(String name, String descriptionKey,
		String defaultValue, Predicate<String> validator)
	{
		this(name, WText.translated(descriptionKey), defaultValue, validator);
	}
	
	public TextFieldSetting(String name, String defaultValue,
		Predicate<String> validator)
	{
		this(name, WText.empty(), defaultValue, validator);
	}
	
	public TextFieldSetting(String name, WText description, String defaultValue)
	{
		this(name, description, defaultValue, s -> true);
	}
	
	public TextFieldSetting(String name, String descriptionKey,
		String defaultValue)
	{
		this(name, WText.translated(descriptionKey), defaultValue, s -> true);
	}
	
	public TextFieldSetting(String name, String defaultValue)
	{
		this(name, WText.empty(), defaultValue, s -> true);
	}
	
	/**
	 * @return this setting's value. Cannot be null.
	 */
	public String getValue()
	{
		return value;
	}
	
	public String getDefaultValue()
	{
		return defaultValue;
	}
	
	/**
	 * Sets this setting's value. Fails silently if the given value is invalid.
	 */
	public void setValue(String value)
	{
		if(value == null)
			return;
		
		if(this.value.equals(value))
			return;
		
		if(!validator.test(value))
			return;
		
		this.value = value;
		WurstClient.INSTANCE.saveSettings();
	}
	
	public void resetToDefault()
	{
		value = defaultValue;
		WurstClient.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new TextFieldEditButton(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			String newValue = JsonUtils.getAsString(json);
			if(!validator.test(newValue))
				throw new JsonException();
			
			value = newValue;
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			resetToDefault();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		return new JsonPrimitive(value);
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", getName());
		json.addProperty("description", getDescription());
		json.addProperty("type", "TextField");
		json.addProperty("defaultValue", defaultValue);
		return json;
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
