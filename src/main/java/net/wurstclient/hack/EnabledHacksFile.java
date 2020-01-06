/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.google.gson.JsonArray;

import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;

public final class EnabledHacksFile
{
	private final Path path;
	private boolean disableSaving;
	
	public EnabledHacksFile(Path path)
	{
		this.path = path;
	}
	
	public void load(HackList hackList)
	{
		try
		{
			WsonArray wson = JsonUtils.parseFileToArray(path);
			enableHacks(hackList, wson);
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
		}
		
		save(hackList);
	}
	
	private void enableHacks(HackList hax, WsonArray wson)
	{
		try
		{
			disableSaving = true;
			
			for(String name : wson.getAllStrings())
			{
				Hack hack = hax.getHackByName(name);
				if(hack == null || !hack.isStateSaved())
					continue;
				
				hack.setEnabled(true);
			}
			
		}finally
		{
			disableSaving = false;
		}
	}
	
	public void save(HackList hax)
	{
		if(disableSaving)
			return;
		
		JsonArray json = createJson(hax);
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	private JsonArray createJson(HackList hax)
	{
		Stream<Hack> enabledHax = hax.getAllHax().stream()
			.filter(Hack::isEnabled).filter(Hack::isStateSaved);
		
		JsonArray json = new JsonArray();
		enabledHax.map(Hack::getName).forEach(name -> json.add(name));
		
		return json;
	}
}
