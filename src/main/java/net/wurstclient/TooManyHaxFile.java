/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;

import com.google.gson.JsonArray;

import net.wurstclient.command.Command;
import net.wurstclient.hack.Hack;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;

public final class TooManyHaxFile
{
	private final Path path;
	private final ArrayList<Feature> hiddenFeatures = new ArrayList<>();
	
	public TooManyHaxFile(Path path)
	{
		this.path = path;
	}
	
	public void load()
	{
		try
		{
			WsonArray wson = JsonUtils.parseFileToArray(path);
			setHiddenFeatures(wson);
			
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
		
		WsonArray wson = JsonUtils.parseFileToArray(profilePath);
		setHiddenFeatures(wson);
		
		save();
	}
	
	private void setHiddenFeatures(WsonArray wson)
	{
		hiddenFeatures.clear();
		
		for(String name : wson.getAllStrings())
		{
			Feature feature = getFeatureByName(name);
			
			if(feature != null && feature.isSafeToHide())
				hiddenFeatures.add(feature);
		}
	}
	
	private Feature getFeatureByName(String name)
	{
		WurstClient wurst = WurstClient.INSTANCE;
		
		Hack hack = wurst.getHax().getHackByName(name);
		if(hack != null)
			return hack;
		
		Command cmd = wurst.getCmds().getCmdByName(name);
		if(cmd != null)
			return cmd;
		
		OtherFeature otf = wurst.getOtfs().getOtfByName(name);
		if(otf != null)
			return otf;
		
		return null;
	}
	
	public void save()
	{
		JsonArray json = createJson();
		
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
		
		JsonArray json = createJson();
		Files.createDirectories(profilePath.getParent());
		JsonUtils.toJson(json, profilePath);
	}
	
	private JsonArray createJson()
	{
		JsonArray json = new JsonArray();
		hiddenFeatures.stream().filter(Feature::isSafeToHide)
			.map(Feature::getName).forEach(name -> json.add(name));
		
		return json;
	}
}
