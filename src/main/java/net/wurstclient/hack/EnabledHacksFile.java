/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import net.wurstclient.utils.JsonUtils;

public final class EnabledHacksFile
{
	private final Path path;
	private boolean disableSaving;
	
	public EnabledHacksFile(Path path)
	{
		this.path = path;
	}
	
	public void load(HackList hackList)
	{
		try
		{
			JsonArray json = parseJson(path);
			enableHacks(hackList, json);
			
		}catch(ConfigFileException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
			
		}catch(NoSuchFileException e)
		{
			
		}
		
		save(hackList);
	}
	
	private JsonArray parseJson(Path path)
		throws NoSuchFileException, ConfigFileException
	{
		try(BufferedReader reader = Files.newBufferedReader(path))
		{
			JsonElement json = JsonUtils.JSON_PARSER.parse(reader);
			if(!json.isJsonArray())
				throw new ConfigFileException();
			
			return json.getAsJsonArray();
			
		}catch(NoSuchFileException e)
		{
			throw e;
			
		}catch(IOException | JsonParseException e)
		{
			throw new ConfigFileException(e);
		}
	}
	
	private void enableHacks(HackList hackList, JsonArray json)
	{
		Stream<JsonElement> jsonElements =
			StreamSupport.stream(json.spliterator(), false);
		
		Stream<String> names = jsonElements.filter(JsonElement::isJsonPrimitive)
			.map(JsonElement::getAsJsonPrimitive)
			.filter(JsonPrimitive::isString).map(JsonPrimitive::getAsString);
		
		Stream<Hack> hacksToEnable =
			names.map(name -> hackList.getHackByName(name))
				.filter(Objects::nonNull).filter(Hack::isStateSaved);
		
		disableSaving = true;
		hacksToEnable.forEach(hack -> hack.setEnabled(true));
		disableSaving = false;
	}
	
	public void save(HackList hax)
	{
		if(disableSaving)
			return;
		
		JsonArray json = createJson(hax);
		
		try(BufferedWriter writer = Files.newBufferedWriter(path))
		{
			JsonUtils.PRETTY_GSON.toJson(json, writer);
			
		}catch(IOException | JsonParseException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonArray createJson(HackList hax)
	{
		Stream<Hack> enabledHax = hax.getAllHax().stream()
			.filter(Hack::isEnabled).filter(Hack::isStateSaved);
		
		JsonArray json = new JsonArray();
		enabledHax.map(Hack::getName).forEach(name -> json.add(name));
		
		return json;
	}
	
	private static final class ConfigFileException extends Exception
	{
		public ConfigFileException()
		{
			super();
		}
		
		public ConfigFileException(Throwable cause)
		{
			super(cause);
		}
	}
}
