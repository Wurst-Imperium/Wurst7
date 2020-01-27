/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.analytics;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import net.wurstclient.analytics.dmurph.VisitorData;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

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
			WsonObject wson = JsonUtils.parseFileToObject(path);
			tracker.setEnabled(wson.getBoolean("enabled"));
			tracker.getConfigData().setVisitorData(readVisitorData(wson));
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
		}
		
		save(tracker);
	}
	
	private VisitorData readVisitorData(WsonObject wson) throws JsonException
	{
		int visitorID = wson.getInt("id");
		long firstLaunch = wson.getLong("first_launch");
		long lastLaunch = wson.getLong("last_launch");
		int launches = wson.getInt("launches");
		
		return VisitorData.newSession(visitorID, firstLaunch, lastLaunch,
			launches);
	}
	
	public void save(WurstAnalyticsTracker tracker)
	{
		JsonObject json = createJson(tracker);
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
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
}
