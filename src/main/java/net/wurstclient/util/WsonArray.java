/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

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
			.filter(JsonElement::isJsonPrimitive)
			.map(JsonElement::getAsJsonPrimitive)
			.filter(JsonPrimitive::isString).map(JsonPrimitive::getAsString)
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
	}
	
	public JsonArray toJsonArray()
	{
		return json;
	}
}
