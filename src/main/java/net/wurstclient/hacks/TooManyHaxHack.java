/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.wurstclient.Category;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.SearchTags;
import net.wurstclient.TooManyHaxFile;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.json.JsonException;

@SearchTags({"too many hax", "TooManyHacks", "too many hacks", "YesCheat+",
	"YesCheatPlus", "yes cheat plus"})
@DontBlock
public final class TooManyHaxHack extends Hack
{
	private final ArrayList<Feature> blockedFeatures = new ArrayList<>();
	private final Path profilesFolder;
	private final TooManyHaxFile file;
	
	public TooManyHaxHack()
	{
		super("TooManyHax",
			"Blocks any features that you don't want.\n"
				+ "Allows you to make sure that you don't accidentally\n"
				+ "enable the wrong hack and get banned for it.\n"
				+ "For those who want to \"only hack a little bit\".\n\n"
				+ "Use the \u00a76.toomanyhax\u00a7r command to choose\n"
				+ "which features to block.\n"
				+ "Type \u00a76.help toomanyhax\u00a7r for more info.");
		setCategory(Category.OTHER);
		
		Path wurstFolder = WURST.getWurstFolder();
		profilesFolder = wurstFolder.resolve("toomanyhax");
		
		Path filePath = wurstFolder.resolve("toomanyhax.json");
		file = new TooManyHaxFile(filePath, blockedFeatures);
	}
	
	public void loadBlockedHacksFile()
	{
		file.load();
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + blockedFeatures.size() + " blocked]";
	}
	
	@Override
	protected void onEnable()
	{
		disableBlockedHacks();
	}
	
	private void disableBlockedHacks()
	{
		for(Feature feature : blockedFeatures)
		{
			if(!(feature instanceof Hack))
				continue;
			
			((Hack)feature).setEnabled(false);
		}
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
		disableBlockedHacks();
	}
	
	public void saveProfile(String fileName) throws IOException, JsonException
	{
		file.saveProfile(profilesFolder.resolve(fileName));
	}
	
	public boolean isBlocked(Feature feature)
	{
		return blockedFeatures.contains(feature);
	}
	
	public void setBlocked(Feature feature, boolean blocked)
	{
		if(blocked)
		{
			if(!feature.isSafeToBlock())
				throw new IllegalArgumentException();
			
			blockedFeatures.add(feature);
			blockedFeatures
				.sort(Comparator.comparing(f -> f.getName().toLowerCase()));
			
		}else
			blockedFeatures.remove(feature);
		
		file.save();
	}
	
	public void blockAll()
	{
		blockedFeatures.clear();
		
		ArrayList<Feature> features = new ArrayList<>();
		features.addAll(WURST.getHax().getAllHax());
		features.addAll(WURST.getCmds().getAllCmds());
		features.addAll(WURST.getOtfs().getAllOtfs());
		
		for(Feature feature : features)
		{
			if(!feature.isSafeToBlock())
				continue;
			
			blockedFeatures.add(feature);
		}
		
		blockedFeatures
			.sort(Comparator.comparing(f -> f.getName().toLowerCase()));
		
		file.save();
	}
	
	public void unblockAll()
	{
		blockedFeatures.clear();
		file.save();
	}
	
	public List<Feature> getBlockedFeatures()
	{
		return Collections.unmodifiableList(blockedFeatures);
	}
}
