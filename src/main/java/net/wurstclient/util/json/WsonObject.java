/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.json;

import java.util.Objects;

import com.google.gson.JsonObject;

public final class WsonObject
{
	private final JsonObject json;
	
	public WsonObject(JsonObject json)
	{
		this.json = Objects.requireNonNull(json);
	}
	
	public boolean getBoolean(String key) throws JsonException
	{
		return JsonUtils.getAsBoolean(json.get(key));
	}
	
	public int getInt(String key) throws JsonException
	{
		return JsonUtils.getAsInt(json.get(key));
	}
	
	public long getLong(String key) throws JsonException
	{
		return JsonUtils.getAsLong(json.get(key));
	}
	
	public JsonObject toJsonObject()
	{
		return json;
	}
}
