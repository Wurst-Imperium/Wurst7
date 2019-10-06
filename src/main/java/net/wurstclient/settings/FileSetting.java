/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public final class FileSetting extends Setting
{
	private final Path folder;
	private String selectedFile = "";
	private final Consumer<Path> createDefaultFiles;
	
	public FileSetting(String name, String description, String folderName,
		Consumer<Path> createDefaultFiles)
	{
		super(name, description);
		folder = WurstClient.INSTANCE.getWurstFolder().resolve(folderName);
		this.createDefaultFiles = createDefaultFiles;
	}
	
	public Path getFolder()
	{
		return folder;
	}
	
	public String getSelectedFileName()
	{
		return selectedFile;
	}
	
	public Path getSelectedFile()
	{
		return folder.resolve(selectedFile);
	}
	
	public void setSelectedFile(String selectedFile)
	{
		Objects.requireNonNull(selectedFile);
		
		Path newSelectedFile = folder.resolve(selectedFile);
		if(!Files.exists(newSelectedFile))
			return;
		
		this.selectedFile = selectedFile;
		WurstClient.INSTANCE.saveSettings();
	}
	
	private void resetToDefault()
	{
		createFolder();
		ArrayList<Path> files = listFiles();
		
		if(files.isEmpty())
		{
			createDefaultFiles.accept(folder);
			files = listFiles();
			
			if(files.isEmpty())
				throw new IllegalStateException(
					"Couldn't generate default files!");
		}
		
		selectedFile = "" + files.get(0).getFileName();
	}
	
	private void createFolder()
	{
		if(Files.isDirectory(folder))
			return;
		
		try
		{
			Files.deleteIfExists(folder);
			Files.createDirectories(folder);
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private ArrayList<Path> listFiles()
	{
		try
		{
			return Files.list(folder)
				.collect(Collectors.toCollection(() -> new ArrayList<>()));
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Component getComponent()
	{
		return null; // TODO
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		try
		{
			String newFile = JsonUtils.getAsString(json);
			
			if(!Files.exists(folder.resolve(newFile)))
				throw new JsonException();
			
			selectedFile = newFile;
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			resetToDefault();
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		return new JsonPrimitive(selectedFile);
	}
	
	@Override
	public Set<PossibleKeybind> getPossibleKeybinds(String featureName)
	{
		return new LinkedHashSet<>();
	}
}
