/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

public final class WsonArray
{
	private final JsonArray json;
	
	public WsonArray(JsonArray json)
	{
		this.json = Objects.requireNonNull(json);
	}
	
	public ArrayList<String> getAllStrings()
	{
		return StreamSupport.stream(json.spliterator(), false)
			.filter(JsonUtils::isString).map(JsonElement::getAsString)
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	public ArrayList<WsonObject> getAllObjects()
	{
		return StreamSupport.stream(json.spliterator(), false)
			.filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject)
			.map(json -> new WsonObject(json))
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	public JsonArray toJsonArray()
	{
		return json;
	}
}
