/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.google.gson.JsonObject;

import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class AltsFile
{
	private final Path path;
	private boolean disableSaving;
	private final Encryption encryption;
	
	public AltsFile(Path path, Path encFolder)
	{
		this.path = path;
		encryption = new Encryption(encFolder);
	}
	
	public void load(AltManager altManager)
	{
		try
		{
			WsonObject wson = encryption.parseFileToObject(path);
			loadAlts(wson, altManager);
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
			
			renameCorrupted();
		}
		
		save(altManager);
	}
	
	private void renameCorrupted()
	{
		try
		{
			Path newPath =
				path.resolveSibling("!CORRUPTED_" + path.getFileName());
			Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Renamed to " + newPath.getFileName());
			
		}catch(IOException e2)
		{
			System.out.println(
				"Couldn't rename corrupted file " + path.getFileName());
			e2.printStackTrace();
		}
	}
	
	private void loadAlts(WsonObject wson, AltManager altManager)
	{
		ArrayList<Alt> alts = new ArrayList<>();
		
		for(Entry<String, JsonObject> e : wson.getAllJsonObjects().entrySet())
		{
			String email = e.getKey();
			JsonObject jsonAlt = e.getValue();
			
			alts.add(loadAlt(email, jsonAlt));
		}
		
		try
		{
			disableSaving = true;
			altManager.addAll(alts);
			
		}finally
		{
			disableSaving = false;
		}
	}
	
	private Alt loadAlt(String email, JsonObject jsonAlt)
	{
		String password = JsonUtils.getAsString(jsonAlt.get("password"), "");
		String name = JsonUtils.getAsString(jsonAlt.get("name"), "");
		boolean starred = JsonUtils.getAsBoolean(jsonAlt.get("starred"), false);
		
		return new Alt(email, password, name, starred);
	}
	
	public void save(AltManager alts)
	{
		if(disableSaving)
			return;
		
		JsonObject json = createJson(alts);
		
		try
		{
			encryption.toEncryptedJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonObject createJson(AltManager alts)
	{
		JsonObject json = new JsonObject();
		
		for(Alt alt : alts.getList())
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
