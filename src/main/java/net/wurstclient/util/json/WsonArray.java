/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.json;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Custom version of {@link JsonArray} that only throws checked exceptions and
 * generally makes it easier to process untrusted JSON data without accidentally
 * crashing something.
 */
public final class WsonArray
{
	private final JsonArray json;
	
	public WsonArray(JsonArray json)
	{
		this.json = Objects.requireNonNull(json);
	}
	
	public boolean getBoolean(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsBoolean(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Boolean at [" + index + "] not found.", e);
		}
	}
	
	public boolean getBoolean(int index, boolean fallback)
	{
		return JsonUtils.getAsBoolean(getElement(index, null), fallback);
	}
	
	public int getInt(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsInt(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number at [" + index + "] not found.", e);
		}
	}
	
	public int getInt(int index, int fallback)
	{
		return JsonUtils.getAsInt(getElement(index, null), fallback);
	}
	
	public long getLong(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsLong(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number at [" + index + "] not found.", e);
		}
	}
	
	public long getLong(int index, long fallback)
	{
		return JsonUtils.getAsLong(getElement(index, null), fallback);
	}
	
	public float getFloat(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsFloat(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number at [" + index + "] not found.", e);
		}
	}
	
	public float getFloat(int index, float fallback)
	{
		return JsonUtils.getAsFloat(getElement(index, null), fallback);
	}
	
	public double getDouble(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsDouble(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Number at [" + index + "] not found.", e);
		}
	}
	
	public double getDouble(int index, double fallback)
	{
		return JsonUtils.getAsDouble(getElement(index, null), fallback);
	}
	
	public String getString(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsString(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("String at [" + index + "] not found.", e);
		}
	}
	
	public String getString(int index, String fallback)
	{
		return JsonUtils.getAsString(getElement(index, null), fallback);
	}
	
	public WsonArray getArray(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsArray(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Array at [" + index + "] not found.", e);
		}
	}
	
	public WsonObject getObject(int index) throws JsonException
	{
		try
		{
			return JsonUtils.getAsObject(getElement(index));
			
		}catch(JsonException e)
		{
			throw new JsonException("Object at [" + index + "] not found.", e);
		}
	}
	
	public JsonElement getElement(int index) throws JsonException
	{
		try
		{
			return json.get(index);
			
		}catch(IndexOutOfBoundsException e)
		{
			throw new JsonException(e.getMessage());
		}
	}
	
	public JsonElement getElement(int index, JsonElement fallback)
	{
		try
		{
			return json.get(index);
			
		}catch(IndexOutOfBoundsException e)
		{
			return fallback;
		}
	}
	
	public ArrayList<String> getAllStrings()
	{
		return StreamSupport.stream(json.spliterator(), false)
			.filter(JsonUtils::isString).map(JsonElement::getAsString)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public ArrayList<WsonObject> getAllObjects()
	{
		return StreamSupport.stream(json.spliterator(), false)
			.filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
			.map(WsonObject::new)
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public int size()
	{
		return json.size();
	}
	
	public boolean isEmpty()
	{
		return json.isEmpty();
	}
	
	public JsonArray toJsonArray()
	{
		return json;
	}
	
	@Override
	public String toString()
	{
		return json.toString();
	}
}
