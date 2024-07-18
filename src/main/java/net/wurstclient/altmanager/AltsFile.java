/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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
	private final Path encFolder;
	private boolean disableSaving;
	private Encryption encryption;
	private IOException folderException;
	
	public AltsFile(Path path, Path encFolder)
	{
		this.path = path;
		this.encFolder = encFolder;
	}
	
	public void load(AltManager altManager)
	{
		try
		{
			if(encryption == null)
				encryption = new Encryption(encFolder);
			
		}catch(IOException e)
		{
			System.out.println("Couldn't create '.Wurst encryption' folder.");
			e.printStackTrace();
			folderException = e;
			return;
		}
		
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
			
		}catch(IOException e)
		{
			System.out.println(
				"Couldn't rename corrupted file " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private void loadAlts(WsonObject wson, AltManager altManager)
	{
		ArrayList<Alt> alts = parseJson(wson);
		
		try
		{
			disableSaving = true;
			altManager.addAll(alts);
			
		}finally
		{
			disableSaving = false;
		}
	}
	
	public static ArrayList<Alt> parseJson(WsonObject wson)
	{
		ArrayList<Alt> alts = new ArrayList<>();
		
		for(Entry<String, JsonObject> e : wson.getAllJsonObjects().entrySet())
		{
			String nameOrEmail = e.getKey();
			if(nameOrEmail.isEmpty())
				continue;
			
			JsonObject jsonAlt = e.getValue();
			alts.add(loadAlt(nameOrEmail, jsonAlt));
		}
		
		return alts;
	}
	
	private static Alt loadAlt(String nameOrEmail, JsonObject jsonAlt)
	{
		String password = JsonUtils.getAsString(jsonAlt.get("password"), "");
		boolean starred = JsonUtils.getAsBoolean(jsonAlt.get("starred"), false);
		
		if(password.isEmpty())
			return new CrackedAlt(nameOrEmail, starred);
		
		String name = JsonUtils.getAsString(jsonAlt.get("name"), "");
		return new MojangAlt(nameOrEmail, password, name, starred);
	}
	
	public void save(AltManager alts)
	{
		if(disableSaving)
			return;
		
		try
		{
			if(encryption == null)
				encryption = new Encryption(encFolder);
			
		}catch(IOException e)
		{
			System.out.println("Couldn't create '.Wurst encryption' folder.");
			e.printStackTrace();
			folderException = e;
			return;
		}
		
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
	
	public static JsonObject createJson(AltManager alts)
	{
		JsonObject json = new JsonObject();
		
		for(Alt alt : alts.getList())
			alt.exportAsJson(json);
		
		return json;
	}
	
	public IOException getFolderException()
	{
		return folderException;
	}
}
