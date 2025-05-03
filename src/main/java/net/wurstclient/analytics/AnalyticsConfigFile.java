/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
	
	public void load(PlausibleAnalytics plausible)
	{
		try
		{
			WsonObject wson = JsonUtils.parseFileToObject(path);
			plausible.setEnabled(wson.getBoolean("enabled"));
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
		}
		
		save(plausible);
	}
	
	public void save(PlausibleAnalytics plausible)
	{
		JsonObject json = createJson(plausible);
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonObject createJson(PlausibleAnalytics plausible)
	{
		JsonObject json = new JsonObject();
		json.addProperty("enabled", plausible.isEnabled());
		return json;
	}
}
