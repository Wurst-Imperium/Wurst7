/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.util.InputUtil;
import net.wurstclient.util.json.JsonUtils;

public final class KeybindList
{
	private final Path path;
	private final ArrayList<Keybind> keybinds = new ArrayList<>();
	
	public KeybindList(Path file)
	{
		path = file;
	}
	
	public void init()
	{
		JsonObject json;
		try(BufferedReader reader = Files.newBufferedReader(path))
		{
			json = JsonUtils.JSON_PARSER.parse(reader).getAsJsonObject();
			
		}catch(NoSuchFileException e)
		{
			loadDefaults();
			return;
			
		}catch(Exception e)
		{
			System.out.println("Failed to load " + path.getFileName());
			e.printStackTrace();
			
			loadDefaults();
			return;
		}
		
		keybinds.clear();
		
		TreeMap<String, String> keybinds2 = new TreeMap<>();
		for(Entry<String, JsonElement> entry : json.entrySet())
		{
			String key = entry.getKey();
			
			// test if key is valid
			try
			{
				InputUtil.fromName(key);
				
			}catch(IllegalArgumentException e)
			{
				continue;
			}
			
			JsonElement value = entry.getValue();
			String commands;
			if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
				commands = value.getAsString();
			else if(value.isJsonArray())
			{
				ArrayList<String> commands2 = new ArrayList<>();
				
				for(JsonElement e : value.getAsJsonArray())
					if(e.isJsonPrimitive() && e.getAsJsonPrimitive().isString())
						commands2.add(e.getAsString());
					
				commands = String.join(";", commands2);
			}else
				continue;
			
			keybinds2.put(key, commands);
		}
		
		for(Entry<String, String> entry : keybinds2.entrySet())
			keybinds.add(new Keybind(entry.getKey(), entry.getValue()));
		
		save();
	}
	
	public void loadDefaults()
	{
		keybinds.clear();
		keybinds.add(new Keybind("key.keyboard.b", "fastplace;fastbreak"));
		keybinds.add(new Keybind("key.keyboard.c", "fullbright"));
		keybinds.add(new Keybind("key.keyboard.g", "flight"));
		keybinds.add(new Keybind("key.keyboard.semicolon", "speednuker"));
		keybinds.add(new Keybind("key.keyboard.h", "/home"));
		keybinds.add(new Keybind("key.keyboard.j", "jesus"));
		keybinds.add(new Keybind("key.keyboard.k", "multiaura"));
		keybinds.add(new Keybind("key.keyboard.n", "nuker"));
		keybinds.add(new Keybind("key.keyboard.r", "killaura"));
		keybinds.add(new Keybind("key.keyboard.right.shift", "navigator"));
		keybinds.add(new Keybind("key.keyboard.right.control", "clickgui"));
		keybinds.add(new Keybind("key.keyboard.u", "freecam"));
		keybinds.add(new Keybind("key.keyboard.x", "x-ray"));
		keybinds.add(new Keybind("key.keyboard.y", "sneak"));
		save();
	}
	
	private void save()
	{
		JsonObject json = new JsonObject();
		for(Keybind keybind : keybinds)
			json.addProperty(keybind.getKey(), keybind.getCommands());
		
		try(BufferedWriter writer = Files.newBufferedWriter(path))
		{
			JsonUtils.PRETTY_GSON.toJson(json, writer);
			
		}catch(IOException e)
		{
			System.out.println("Failed to save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	public int size()
	{
		return keybinds.size();
	}
	
	public Keybind get(int index)
	{
		return keybinds.get(index);
	}
	
	public String getCommands(String key)
	{
		for(Keybind keybind : keybinds)
		{
			if(!key.equals(keybind.getKey()))
				continue;
			
			return keybind.getCommands();
		}
		
		return null;
	}
	
	public void add(String key, String commands)
	{
		keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
		keybinds.add(new Keybind(key, commands));
		keybinds.sort(Comparator.comparing(Keybind::getKey));
		save();
	}
	
	public void remove(String key)
	{
		keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
		save();
	}
	
	public void removeAll()
	{
		keybinds.clear();
		save();
	}
}
