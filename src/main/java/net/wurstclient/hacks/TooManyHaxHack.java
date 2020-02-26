/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.wurstclient.Category;
import net.wurstclient.DontHide;
import net.wurstclient.Feature;
import net.wurstclient.SearchTags;
import net.wurstclient.TooManyHaxFile;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.json.JsonException;

@SearchTags({"too many hax", "TooManyHacks", "too many hacks"})
@DontHide
public final class TooManyHaxHack extends Hack
{
	private final ArrayList<Feature> hiddenFeatures = new ArrayList<>();
	private final Path profilesFolder;
	private final TooManyHaxFile file;
	
	public TooManyHaxHack()
	{
		super("TooManyHax",
			"Hides and disables features that you don't want.\n"
				+ "For those who want to \"only hack a little bit\".\n\n"
				+ "Use the \u00a76.toomanyhax\u00a7r command to choose\n"
				+ "which features to hide.\n"
				+ "Type \u00a76.help toomanyhax\u00a7r for more info.");
		setCategory(Category.OTHER);
		
		Path wurstFolder = WURST.getWurstFolder();
		profilesFolder = wurstFolder.resolve("toomanyhax");
		
		Path filePath = wurstFolder.resolve("toomanyhax.json");
		file = new TooManyHaxFile(filePath, hiddenFeatures);
		file.load();
	}
	
	public ArrayList<Path> listProfiles()
	{
		if(!Files.isDirectory(profilesFolder))
			return new ArrayList<>();
		
		try(Stream<Path> files = Files.list(profilesFolder))
		{
			return files.filter(Files::isRegularFile)
				.collect(Collectors.toCollection(() -> new ArrayList<>()));
			
		}catch(IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void loadProfile(String fileName) throws IOException, JsonException
	{
		file.loadProfile(profilesFolder.resolve(fileName));
	}
	
	public void saveProfile(String fileName) throws IOException, JsonException
	{
		file.saveProfile(profilesFolder.resolve(fileName));
	}
	
	public boolean isHidden(Feature feature)
	{
		return hiddenFeatures.contains(feature);
	}
	
	public void setHidden(Feature feature, boolean hidden)
	{
		if(hidden)
			hiddenFeatures.add(feature);
		else
			hiddenFeatures.remove(feature);
		
		file.save();
	}
	
	public void unhideAll()
	{
		hiddenFeatures.clear();
		file.save();
	}
	
	public List<Feature> getHiddenFeatures()
	{
		return Collections.unmodifiableList(hiddenFeatures);
	}
}
