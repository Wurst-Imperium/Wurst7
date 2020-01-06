/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonObject;

import net.minecraft.client.util.InputUtil;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class KeybindsFile
{
	private final Path path;
	
	public KeybindsFile(Path path)
	{
		this.path = path;
	}
	
	public void load(KeybindList list)
	{
		try
		{
			WsonObject wson = JsonUtils.parseFileToObject(path);
			Set<Keybind> newKeybinds = new HashSet<>();
			
			for(Entry<String, String> entry : wson.getAllStrings().entrySet())
			{
				String keyName = entry.getKey();
				String commands = entry.getValue();
				
				if(!isValidKeyName(keyName))
					continue;
				
				Keybind keybind = new Keybind(keyName, commands);
				newKeybinds.add(keybind);
			}
			
			if(newKeybinds.isEmpty())
				newKeybinds = KeybindList.DEFAULT_KEYBINDS;
			
			list.setKeybinds(newKeybinds);
			
		}catch(NoSuchFileException e)
		{
			list.setKeybinds(KeybindList.DEFAULT_KEYBINDS);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
			
			list.setKeybinds(KeybindList.DEFAULT_KEYBINDS);
		}
	}
	
	private boolean isValidKeyName(String key)
	{
		try
		{
			InputUtil.fromName(key);
			return true;
			
		}catch(IllegalArgumentException e)
		{
			return false;
		}
	}
	
	public void save(KeybindList list)
	{
		JsonObject json = createJson(list);
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonObject createJson(KeybindList list)
	{
		JsonObject json = new JsonObject();
		
		for(Keybind kb : list.getAllKeybinds())
			json.addProperty(kb.getKey(), kb.getCommands());
		
		return json;
	}
}
