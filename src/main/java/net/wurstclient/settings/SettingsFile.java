/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.wurstclient.Feature;
import net.wurstclient.command.CmdList;
import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;
import net.wurstclient.other_feature.OtfList;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class SettingsFile
{
	private final Path path;
	private final Map<String, Feature> featuresWithSettings;
	private boolean disableSaving;
	
	public SettingsFile(Path path, HackList hax, CmdList cmds, OtfList otfs)
	{
		this.path = path;
		featuresWithSettings = createFeatureMap(hax, cmds, otfs);
	}
	
	private Map<String, Feature> createFeatureMap(HackList hax, CmdList cmds,
		OtfList otfs)
	{
		LinkedHashMap<String, Feature> map = new LinkedHashMap<>();
		
		for(Hack hack : hax.getAllHax())
			if(!hack.getSettings().isEmpty())
				map.put(hack.getName(), hack);
			
		for(Command cmd : cmds.getAllCmds())
			if(!cmd.getSettings().isEmpty())
				map.put(cmd.getName(), cmd);
			
		for(OtherFeature otf : otfs.getAllOtfs())
			if(!otf.getSettings().isEmpty())
				map.put(otf.getName(), otf);
			
		return Collections.unmodifiableMap(map);
	}
	
	public void load()
	{
		try
		{
			WsonObject wson = JsonUtils.parseFileToObject(path);
			loadSettings(wson);
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
		}
		
		save();
	}
	
	public void loadProfile(Path profilePath) throws IOException, JsonException
	{
		if(!profilePath.getFileName().toString().endsWith(".json"))
			throw new IllegalArgumentException();
		
		WsonObject wson = JsonUtils.parseFileToObject(profilePath);
		loadSettings(wson);
		
		save();
	}
	
	private void loadSettings(WsonObject wson)
	{
		try
		{
			disableSaving = true;
			
			for(Entry<String, JsonObject> e : wson.getAllJsonObjects()
				.entrySet())
			{
				Feature feature = featuresWithSettings.get(e.getKey());
				if(feature == null)
					continue;
				
				loadSettings(feature, e.getValue());
			}
			
		}finally
		{
			disableSaving = false;
		}
	}
	
	private void loadSettings(Feature feature, JsonObject json)
	{
		Map<String, Setting> settings = feature.getSettings();
		
		for(Entry<String, JsonElement> e : json.entrySet())
		{
			String key = e.getKey().toLowerCase();
			if(!settings.containsKey(key))
				continue;
			
			settings.get(key).fromJson(e.getValue());
		}
	}
	
	public void save()
	{
		if(disableSaving)
			return;
		
		JsonObject json = createJson();
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	public void saveProfile(Path profilePath) throws IOException, JsonException
	{
		if(!profilePath.getFileName().toString().endsWith(".json"))
			throw new IllegalArgumentException();
		
		JsonObject json = createJson();
		Files.createDirectories(profilePath.getParent());
		JsonUtils.toJson(json, profilePath);
	}
	
	private JsonObject createJson()
	{
		JsonObject json = new JsonObject();
		
		for(Feature feature : featuresWithSettings.values())
		{
			Collection<Setting> settings = feature.getSettings().values();
			
			JsonObject jsonSettings = new JsonObject();
			settings.forEach(s -> jsonSettings.add(s.getName(), s.toJson()));
			
			json.add(feature.getName(), jsonSettings);
		}
		
		return json;
	}
}
