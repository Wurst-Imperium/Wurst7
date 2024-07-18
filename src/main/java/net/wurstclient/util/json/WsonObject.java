/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.json;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Custom version of {@link JsonObject} that only throws checked exceptions and
 * generally makes it easier to process untrusted JSON data without accidentally
 * crashing something.
 */
public final class WsonObject
{
	private final JsonObject json;
	
	public WsonObject(JsonObject json)
	{
		this.json = Objects.requireNonNull(json);
	}
	
	public boolean getBoolean(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsBoolean(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Boolean \"" + key + "\" not found.", e);
		}
	}
	
	public boolean getBoolean(String key, boolean fallback)
	{
		return JsonUtils.getAsBoolean(json.get(key), fallback);
	}
	
	public int getInt(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsInt(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number \"" + key + "\" not found.", e);
		}
	}
	
	public int getInt(String key, int fallback)
	{
		return JsonUtils.getAsInt(json.get(key), fallback);
	}
	
	public long getLong(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsLong(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number \"" + key + "\" not found.", e);
		}
	}
	
	public long getLong(String key, long fallback)
	{
		return JsonUtils.getAsLong(json.get(key), fallback);
	}
	
	public float getFloat(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsFloat(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number \"" + key + "\" not found.", e);
		}
	}
	
	public float getFloat(String key, float fallback)
	{
		return JsonUtils.getAsFloat(json.get(key), fallback);
	}
	
	public double getDouble(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsDouble(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number \"" + key + "\" not found.", e);
		}
	}
	
	public double getDouble(String key, double fallback)
	{
		return JsonUtils.getAsDouble(json.get(key), fallback);
	}
	
	public String getString(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsString(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("String \"" + key + "\" not found.", e);
		}
	}
	
	public String getString(String key, String fallback)
	{
		return JsonUtils.getAsString(json.get(key), fallback);
	}
	
	public WsonArray getArray(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsArray(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Array \"" + key + "\" not found.", e);
		}
	}
	
	public WsonObject getObject(String key) throws JsonException
	{
		try
		{
			return JsonUtils.getAsObject(json.get(key));
			
		}catch(JsonException e)
		{
			throw new JsonException("Object \"" + key + "\" not found.", e);
		}
	}
	
	public JsonElement getElement(String key)
	{
		return json.get(key);
	}
	
	public LinkedHashMap<String, String> getAllStrings()
	{
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		
		for(Entry<String, JsonElement> entry : json.entrySet())
		{
			JsonElement value = entry.getValue();
			if(!JsonUtils.isString(value))
				continue;
			
			map.put(entry.getKey(), value.getAsString());
		}
		
		return map;
	}
	
	public LinkedHashMap<String, Number> getAllNumbers()
	{
		LinkedHashMap<String, Number> map = new LinkedHashMap<>();
		
		for(Entry<String, JsonElement> entry : json.entrySet())
		{
			JsonElement value = entry.getValue();
			if(!JsonUtils.isNumber(value))
				continue;
			
			map.put(entry.getKey(), value.getAsNumber());
		}
		
		return map;
	}
	
	public LinkedHashMap<String, JsonObject> getAllJsonObjects()
	{
		LinkedHashMap<String, JsonObject> map = new LinkedHashMap<>();
		
		for(Entry<String, JsonElement> entry : json.entrySet())
		{
			JsonElement value = entry.getValue();
			if(!value.isJsonObject())
				continue;
			
			map.put(entry.getKey(), value.getAsJsonObject());
		}
		
		return map;
	}
	
	public boolean has(String memberName)
	{
		return json.has(memberName);
	}
	
	public JsonObject toJsonObject()
	{
		return json;
	}
	
	@Override
	public String toString()
	{
		return json.toString();
	}
}
