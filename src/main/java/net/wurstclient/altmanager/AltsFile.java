/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.google.gson.JsonObject;

import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class AltsFile
{
	private final Path path;
	private final Encryption encryption;
	
	public AltsFile(Path path, Path encFolder)
	{
		this.path = path;
		encryption = new Encryption(encFolder);
	}
	
	public void load(AltList altList)
	{
		try
		{
			WsonObject wson = encryption.parseFileToObject(path);
			ArrayList<Alt> alts = loadAlts(wson);
			altList.addAll(alts);
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
		}
		
		save(altList);
	}
	
	private ArrayList<Alt> loadAlts(WsonObject wson)
	{
		ArrayList<Alt> alts = new ArrayList<>();
		
		for(Entry<String, JsonObject> e : wson.getAllJsonObjects().entrySet())
		{
			String email = e.getKey();
			JsonObject jsonAlt = e.getValue();
			
			alts.add(loadAlt(email, jsonAlt));
		}
		
		return alts;
	}
	
	private Alt loadAlt(String email, JsonObject jsonAlt)
	{
		String password = JsonUtils.getAsString(jsonAlt.get("password"), "");
		String name = JsonUtils.getAsString(jsonAlt.get("name"), "");
		boolean starred = JsonUtils.getAsBoolean(jsonAlt.get("starred"), false);
		
		return new Alt(email, password, name, starred);
	}
	
	public void save(AltList alts)
	{
		JsonObject json = createJson(alts);
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonObject createJson(AltList alts)
	{
		JsonObject json = new JsonObject();
		
		for(Alt alt : alts)
		{
			JsonObject jsonAlt = new JsonObject();
			
			jsonAlt.addProperty("password", alt.getPassword());
			jsonAlt.addProperty("name", alt.getName());
			jsonAlt.addProperty("starred", alt.isStarred());
			
			json.add(alt.getEmail(), jsonAlt);
		}
		
		return json;
	}
}
