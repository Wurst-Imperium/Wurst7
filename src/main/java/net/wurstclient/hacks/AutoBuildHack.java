/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.wurstclient.Category;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener
{
	private final FileSetting template =
		new FileSetting("Template", "Determines what to build.", "autobuild",
			folder -> createDefaultTemplates(folder));
	
	private int[][] blocks;
	private String loadedTemplateName = "";
	
	public AutoBuildHack()
	{
		super("AutoBuild", "Builds things automatically.");
		setCategory(Category.BLOCKS);
		addSetting(template);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		if(!loadedTemplateName.isEmpty())
			name += " [" + loadedTemplateName + "]";
		
		return name;
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RightClickListener.class, this);
		loadSelectedTemplate();
	}
	
	private void loadSelectedTemplate()
	{
		Path path = template.getSelectedFile();
		
		try
		{
			loadTemplate(path);
			System.out.println("Loaded template '" + loadedTemplateName
				+ "' with " + blocks.length + " blocks.");
			
		}catch(IOException | JsonException e)
		{
			Path fileName = path.getFileName();
			ChatUtils.error("Couldn't load template '" + fileName + "'.");
			
			String simpleClassName = e.getClass().getSimpleName();
			String message = e.getMessage();
			ChatUtils.message(simpleClassName + ": " + message);
			
			e.printStackTrace();
			setEnabled(false);
		}
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
	}
	
	private void loadTemplate(Path path) throws IOException, JsonException
	{
		JsonObject json = JsonUtils.parseFileToObject(path).toJsonObject();
		int[][] blocks =
			JsonUtils.GSON.fromJson(json.get("blocks"), int[][].class);
		
		for(int i = 0; i < blocks.length; i++)
		{
			int length = blocks[i].length;
			
			if(length < 3)
				throw new JsonException("Entry blocks[" + i
					+ "] doesn't have X, Y and Z offset. Only found " + length
					+ " values");
		}
		
		String fileName = template.getSelectedFileName();
		loadedTemplateName = fileName.substring(0, fileName.lastIndexOf("."));
		this.blocks = blocks;
	}
	
	private void createDefaultTemplates(Path folder)
	{
		for(DefaultTemplate template : DefaultTemplate.values())
		{
			JsonObject json = createJson(template);
			
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
	
	private JsonObject createJson(DefaultTemplate template)
	{
		JsonObject json = new JsonObject();
		JsonElement blocks = JsonUtils.GSON.toJsonTree(template.data);
		json.add("blocks", blocks);
		
		return json;
	}
	
	private static enum DefaultTemplate
	{
		BRIDGE("Bridge",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, -1}, {0, 0, -1},
				{-1, 0, -1}, {-1, 0, 0}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2},
				{1, 0, -3}, {0, 0, -3}, {-1, 0, -3}, {-1, 0, -4}, {0, 0, -4},
				{1, 0, -4}, {1, 0, -5}, {0, 0, -5}, {-1, 0, -5}}),
		
		FLOOR("Floor",
			new int[][]{{0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {1, 0, -1},
				{0, 0, -1}, {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {-1, 0, 2},
				{0, 0, 2}, {1, 0, 2}, {2, 0, 2}, {2, 0, 1}, {2, 0, 0},
				{2, 0, -1}, {2, 0, -2}, {1, 0, -2}, {0, 0, -2}, {-1, 0, -2},
				{-2, 0, -2}, {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1}, {-2, 0, 2},
				{-2, 0, 3}, {-1, 0, 3}, {0, 0, 3}, {1, 0, 3}, {2, 0, 3},
				{3, 0, 3}, {3, 0, 2}, {3, 0, 1}, {3, 0, 0}, {3, 0, -1},
				{3, 0, -2}, {3, 0, -3}, {2, 0, -3}, {1, 0, -3}, {0, 0, -3},
				{-1, 0, -3}, {-2, 0, -3}, {-3, 0, -3}, {-3, 0, -2}, {-3, 0, -1},
				{-3, 0, 0}, {-3, 0, 1}, {-3, 0, 2}, {-3, 0, 3}}),
		
		PILLAR("Pillar",
			new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 2, 0}, {0, 3, 0}, {0, 4, 0},
				{0, 5, 0}, {0, 6, 0}}),
		
		WALL("Wall",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {-1, 1, 0},
				{-1, 0, 0}, {-2, 0, 0}, {-2, 1, 0}, {-2, 2, 0}, {-1, 2, 0},
				{0, 2, 0}, {1, 2, 0}, {2, 2, 0}, {2, 1, 0}, {2, 0, 0},
				{3, 0, 0}, {3, 1, 0}, {3, 2, 0}, {3, 3, 0}, {2, 3, 0},
				{1, 3, 0}, {0, 3, 0}, {-1, 3, 0}, {-2, 3, 0}, {-3, 3, 0},
				{-3, 2, 0}, {-3, 1, 0}, {-3, 0, 0}, {-3, 4, 0}, {-2, 4, 0},
				{-1, 4, 0}, {0, 4, 0}, {1, 4, 0}, {2, 4, 0}, {3, 4, 0},
				{3, 5, 0}, {2, 5, 0}, {1, 5, 0}, {0, 5, 0}, {-1, 5, 0},
				{-2, 5, 0}, {-3, 5, 0}, {-3, 6, 0}, {-2, 6, 0}, {-1, 6, 0},
				{0, 6, 0}, {1, 6, 0}, {2, 6, 0}, {3, 6, 0}}),
		
		WURST("Wurst",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 1, 1},
				{1, 1, 1}, {2, 1, 1}, {2, 1, 0}, {2, 0, 0}, {2, 1, -1},
				{1, 1, -1}, {0, 1, -1}, {-1, 1, -1}, {-1, 1, 0}, {-1, 0, 0},
				{-2, 0, 0}, {-2, 1, 0}, {-2, 1, 1}, {-1, 1, 1}, {-1, 2, 0},
				{0, 2, 0}, {1, 2, 0}, {2, 2, 0}, {3, 1, 0}, {-2, 1, -1},
				{-2, 2, 0}, {-3, 1, 0}});
		
		private final String name;
		private final int[][] data;
		
		private DefaultTemplate(String name, int[][] data)
		{
			this.name = name;
			this.data = data;
		}
	}
}
