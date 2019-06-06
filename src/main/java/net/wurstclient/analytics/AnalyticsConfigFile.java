/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.analytics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.wurstclient.analytics.dmurph.VisitorData;
import net.wurstclient.utils.JsonUtils;

public final class AnalyticsConfigFile
{
	private final Path path;
	
	public AnalyticsConfigFile(Path path)
	{
		this.path = path;
	}
	
	public void load(WurstAnalyticsTracker tracker)
	{
		try
		{
			JsonElement json = parseJson(path);
			tracker.setEnabled(readEnabled(json));
			tracker.getConfigData().setVisitorData(readVisitorData(json));
			
		}catch(ConfigFileException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
			
		}catch(NoSuchFileException e)
		{
			
		}
		
		save(tracker);
	}
	
	private JsonElement parseJson(Path path)
		throws NoSuchFileException, ConfigFileException
	{
		try(BufferedReader reader = Files.newBufferedReader(path))
		{
			return JsonUtils.JSON_PARSER.parse(reader);
			
		}catch(NoSuchFileException e)
		{
			throw e;
			
		}catch(IOException | JsonParseException e)
		{
			throw new ConfigFileException(e);
		}
	}
	
	private boolean readEnabled(JsonElement json) throws ConfigFileException
	{
		try
		{
			return json.getAsJsonObject().get("enabled").getAsBoolean();
			
		}catch(Exception e)
		{
			throw new ConfigFileException(e);
		}
	}
	
	private VisitorData readVisitorData(JsonElement json)
		throws ConfigFileException
	{
		int visitorID;
		long firstLaunch;
		long lastLaunch;
		int launches;
		
		try
		{
			JsonObject jo = json.getAsJsonObject();
			visitorID = jo.get("id").getAsInt();
			firstLaunch = jo.get("first_launch").getAsLong();
			lastLaunch = jo.get("last_launch").getAsLong();
			launches = jo.get("launches").getAsInt();
			
		}catch(Exception e)
		{
			throw new ConfigFileException(e);
		}
		
		return VisitorData.newSession(visitorID, firstLaunch, lastLaunch,
			launches);
	}
	
	public void save(WurstAnalyticsTracker tracker)
	{
		JsonObject json = createJson(tracker);
		
		try(BufferedWriter writer = Files.newBufferedWriter(path))
		{
			JsonUtils.PRETTY_GSON.toJson(json, writer);
			
		}catch(IOException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonObject createJson(WurstAnalyticsTracker tracker)
	{
		JsonObject json = new JsonObject();
		json.addProperty("enabled", tracker.isEnabled());
		
		VisitorData visitorData = tracker.getConfigData().getVisitorData();
		json.addProperty("id", visitorData.getVisitorId());
		json.addProperty("first_launch", visitorData.getTimestampFirst());
		json.addProperty("last_launch", visitorData.getTimestampCurrent());
		json.addProperty("launches", visitorData.getVisits());
		
		return json;
	}
	
	private static final class ConfigFileException extends Exception
	{
		public ConfigFileException(Throwable cause)
		{
			super(cause);
		}
	}
}
