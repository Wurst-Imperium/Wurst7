/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
		return JsonUtils.getAsBoolean(json.get(index));
	}
	
	public int getInt(int index) throws JsonException
	{
		return JsonUtils.getAsInt(json.get(index));
	}
	
	public long getLong(int index) throws JsonException
	{
		return JsonUtils.getAsLong(json.get(index));
	}
	
	public String getString(int index) throws JsonException
	{
		return JsonUtils.getAsString(json.get(index));
	}
	
	public WsonArray getArray(int index) throws JsonException
	{
		return JsonUtils.getAsArray(json.get(index));
	}
	
	public WsonObject getObject(int index) throws JsonException
	{
		return JsonUtils.getAsObject(json.get(index));
	}
	
	public JsonElement getElement(int index)
	{
		return json.get(index);
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
	
	public JsonArray toJsonArray()
	{
		return json;
	}
}
