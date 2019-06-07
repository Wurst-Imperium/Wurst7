/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.*;

public enum JsonUtils
{
	;
	
	public static final Gson GSON = new Gson();
	
	public static final Gson PRETTY_GSON =
		new GsonBuilder().setPrettyPrinting().create();
	
	public static final JsonParser JSON_PARSER = new JsonParser();
	
	public static JsonElement parse(Path path) throws IOException, JsonException
	{
		try(BufferedReader reader = Files.newBufferedReader(path))
		{
			return JSON_PARSER.parse(reader);
			
		}catch(JsonParseException e)
		{
			throw new JsonException(e);
		}
	}
	
	public static JsonArray parseJsonArray(Path path)
		throws IOException, JsonException
	{
		JsonElement json = parse(path);
		
		if(!json.isJsonArray())
			throw new JsonException();
		
		return json.getAsJsonArray();
	}
	
	public static WsonArray parseWsonArray(Path path)
		throws IOException, JsonException
	{
		return new WsonArray(parseJsonArray(path));
	}
	
	public static JsonObject parseJsonObject(Path path)
		throws IOException, JsonException
	{
		JsonElement json = parse(path);
		
		if(!json.isJsonObject())
			throw new JsonException();
		
		return json.getAsJsonObject();
	}
	
	public static WsonObject parseWsonObject(Path path)
		throws IOException, JsonException
	{
		return new WsonObject(parseJsonObject(path));
	}
	
	public static void toJson(JsonElement json, Path path)
		throws IOException, JsonException
	{
		try(BufferedWriter writer = Files.newBufferedWriter(path))
		{
			JsonUtils.PRETTY_GSON.toJson(json, writer);
			
		}catch(JsonParseException e)
		{
			throw new JsonException(e);
		}
	}
	
	public static boolean isBoolean(JsonElement json)
	{
		if(json == null || !json.isJsonPrimitive())
			return false;
		
		JsonPrimitive primitive = json.getAsJsonPrimitive();
		return primitive.isBoolean();
	}
	
	public static boolean getAsBoolean(JsonElement json) throws JsonException
	{
		if(!isBoolean(json))
			throw new JsonException();
		
		return json.getAsBoolean();
	}
	
	public static boolean isNumber(JsonElement json)
	{
		if(json == null || !json.isJsonPrimitive())
			return false;
		
		JsonPrimitive primitive = json.getAsJsonPrimitive();
		return primitive.isNumber();
	}
	
	public static int getAsInt(JsonElement json) throws JsonException
	{
		if(!isNumber(json))
			throw new JsonException();
		
		return json.getAsInt();
	}
	
	public static long getAsLong(JsonElement json) throws JsonException
	{
		if(!isNumber(json))
			throw new JsonException();
		
		return json.getAsLong();
	}
}
