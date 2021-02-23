/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public enum DefaultAutoBuildTemplates
{
	BRIDGE("Bridge",
		new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, -1}, {0, 0, -1}, {-1, 0, -1},
			{-1, 0, 0}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2}, {1, 0, -3},
			{0, 0, -3}, {-1, 0, -3}, {-1, 0, -4}, {0, 0, -4}, {1, 0, -4},
			{1, 0, -5}, {0, 0, -5}, {-1, 0, -5}}),
	
	FLOOR("Floor", new int[][]{{0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0},
		{1, 0, -1}, {0, 0, -1}, {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {-1, 0, 2},
		{0, 0, 2}, {1, 0, 2}, {2, 0, 2}, {2, 0, 1}, {2, 0, 0}, {2, 0, -1},
		{2, 0, -2}, {1, 0, -2}, {0, 0, -2}, {-1, 0, -2}, {-2, 0, -2},
		{-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1}, {-2, 0, 2}, {-2, 0, 3}, {-1, 0, 3},
		{0, 0, 3}, {1, 0, 3}, {2, 0, 3}, {3, 0, 3}, {3, 0, 2}, {3, 0, 1},
		{3, 0, 0}, {3, 0, -1}, {3, 0, -2}, {3, 0, -3}, {2, 0, -3}, {1, 0, -3},
		{0, 0, -3}, {-1, 0, -3}, {-2, 0, -3}, {-3, 0, -3}, {-3, 0, -2},
		{-3, 0, -1}, {-3, 0, 0}, {-3, 0, 1}, {-3, 0, 2}, {-3, 0, 3}}),
	
	PENIS("Penis",
		new int[][]{{0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0},
			{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 2, 1}, {0, 2, 1}, {0, 2, 0},
			{1, 2, 0}, {1, 3, 0}, {0, 3, 0}, {0, 3, 1}, {1, 3, 1}, {1, 4, 1},
			{0, 4, 1}, {0, 4, 0}, {1, 4, 0}, {1, 5, 0}, {0, 5, 0}, {0, 5, 1},
			{1, 5, 1}, {1, 6, 1}, {0, 6, 1}, {0, 6, 0}, {1, 6, 0}, {1, 7, 0},
			{0, 7, 0}, {0, 7, 1}, {1, 7, 1}, {-1, 0, -1}, {-1, 1, -1},
			{-2, 1, -1}, {-2, 0, -1}, {-2, 0, -2}, {-1, 0, -2}, {-1, 1, -2},
			{-2, 1, -2}, {2, 0, -1}, {2, 1, -1}, {2, 1, -2}, {2, 0, -2},
			{3, 0, -2}, {3, 0, -1}, {3, 1, -1}, {3, 1, -2}}),
	
	PILLAR("Pillar",
		new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 2, 0}, {0, 3, 0}, {0, 4, 0},
			{0, 5, 0}, {0, 6, 0}}),
	
	SWASTIKA("Swastika",
		new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0},
			{1, 2, 0}, {2, 2, 0}, {2, 3, 0}, {2, 4, 0}, {0, 3, 0}, {0, 4, 0},
			{-1, 4, 0}, {-2, 4, 0}, {-1, 2, 0}, {-2, 2, 0}, {-2, 1, 0},
			{-2, 0, 0}}),
	
	TREE("Tree", new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 2, 0}, {0, 3, 0},
		{0, 4, 0}, {0, 3, -1}, {0, 3, 1}, {-1, 3, 0}, {1, 3, 0}, {0, 5, 0},
		{0, 4, -1}, {0, 4, 1}, {-1, 4, 0}, {1, 4, 0}, {0, 3, -2}, {-1, 3, -1},
		{1, 3, -1}, {0, 3, 2}, {-1, 3, 1}, {1, 3, 1}, {-2, 3, 0}, {2, 3, 0},
		{0, 6, 0}, {0, 5, -1}, {0, 5, 1}, {-1, 5, 0}, {1, 5, 0}, {0, 4, -2},
		{-1, 4, -1}, {1, 4, -1}, {0, 4, 2}, {-1, 4, 1}, {1, 4, 1}, {-2, 4, 0},
		{2, 4, 0}, {-1, 3, -2}, {1, 3, -2}, {-2, 3, -1}, {2, 3, -1}, {-1, 3, 2},
		{1, 3, 2}, {-2, 3, 1}, {2, 3, 1}, {0, 6, -1}, {0, 6, 1}, {-1, 6, 0},
		{1, 6, 0}, {1, 5, 1}, {-1, 4, -2}, {1, 4, -2}, {-2, 4, -1}, {2, 4, -1},
		{-1, 4, 2}, {1, 4, 2}, {-2, 4, 1}, {2, 4, 1}, {-2, 3, -2}, {2, 3, -2},
		{2, 3, 2}, {2, 4, -2}}),
	
	WALL("Wall", new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
		{-1, 1, 0}, {-1, 0, 0}, {-2, 0, 0}, {-2, 1, 0}, {-2, 2, 0}, {-1, 2, 0},
		{0, 2, 0}, {1, 2, 0}, {2, 2, 0}, {2, 1, 0}, {2, 0, 0}, {3, 0, 0},
		{3, 1, 0}, {3, 2, 0}, {3, 3, 0}, {2, 3, 0}, {1, 3, 0}, {0, 3, 0},
		{-1, 3, 0}, {-2, 3, 0}, {-3, 3, 0}, {-3, 2, 0}, {-3, 1, 0}, {-3, 0, 0},
		{-3, 4, 0}, {-2, 4, 0}, {-1, 4, 0}, {0, 4, 0}, {1, 4, 0}, {2, 4, 0},
		{3, 4, 0}, {3, 5, 0}, {2, 5, 0}, {1, 5, 0}, {0, 5, 0}, {-1, 5, 0},
		{-2, 5, 0}, {-3, 5, 0}, {-3, 6, 0}, {-2, 6, 0}, {-1, 6, 0}, {0, 6, 0},
		{1, 6, 0}, {2, 6, 0}, {3, 6, 0}}),
	
	WURST("Wurst", new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
		{0, 1, 1}, {1, 1, 1}, {2, 1, 1}, {2, 1, 0}, {2, 0, 0}, {2, 1, -1},
		{1, 1, -1}, {0, 1, -1}, {-1, 1, -1}, {-1, 1, 0}, {-1, 0, 0}, {-2, 0, 0},
		{-2, 1, 0}, {-2, 1, 1}, {-1, 1, 1}, {-1, 2, 0}, {0, 2, 0}, {1, 2, 0},
		{2, 2, 0}, {3, 1, 0}, {-2, 1, -1}, {-2, 2, 0}, {-3, 1, 0}});
	
	private final String name;
	private final int[][] data;
	
	private DefaultAutoBuildTemplates(String name, int[][] data)
	{
		this.name = name;
		this.data = data;
	}
	
	public static void createFiles(Path folder)
	{
		for(DefaultAutoBuildTemplates template : DefaultAutoBuildTemplates
			.values())
		{
			JsonObject json = toJson(template);
			Path path = folder.resolve(template.name + ".json");
			
			try
			{
				JsonUtils.toJson(json, path);
				
			}catch(IOException | JsonException e)
			{
				System.out.println("Couldn't save " + path.getFileName());
				e.printStackTrace();
			}
		}
	}
	
	private static JsonObject toJson(DefaultAutoBuildTemplates template)
	{
		JsonObject json = new JsonObject();
		JsonElement blocks = JsonUtils.GSON.toJsonTree(template.data);
		json.add("blocks", blocks);
		
		return json;
	}
}
