/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.navigator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import net.wurstclient.Feature;
import net.wurstclient.command.CmdList;
import net.wurstclient.hack.HackList;
import net.wurstclient.other_feature.OtfList;

public final class Navigator
{
	private final ArrayList<Feature> navigatorList = new ArrayList<>();
	private final HashMap<String, Long> preferences = new HashMap<>();
	private final PreferencesFile preferencesFile;
	
	public Navigator(Path path, HackList hax, CmdList cmds, OtfList otfs)
	{
		navigatorList.addAll(hax.getAllHax());
		navigatorList.addAll(cmds.getAllCmds());
		navigatorList.addAll(otfs.getAllOtfs());
		
		preferencesFile = new PreferencesFile(path, preferences);
		preferencesFile.load();
	}
	
	public void copyNavigatorList(ArrayList<Feature> list)
	{
		if(list.equals(navigatorList))
			return;
		
		list.clear();
		list.addAll(navigatorList);
	}
	
	public void getSearchResults(ArrayList<Feature> list, String query)
	{
		// clear display list
		list.clear();
		
		// add search results
		for(Feature mod : navigatorList)
			if(mod.getName().toLowerCase().contains(query)
				|| mod.getSearchTags().toLowerCase().contains(query)
				|| mod.getDescription().toLowerCase().contains(query))
				list.add(mod);
			
		Comparator<String> c = (o1, o2) -> {
			int index1 = o1.toLowerCase().indexOf(query);
			int index2 = o2.toLowerCase().indexOf(query);
			
			if(index1 == index2)
				return 0;
			else if(index1 == -1)
				return 1;
			else if(index2 == -1)
				return -1;
			else
				return index1 - index2;
		};
		
		// sort search results
		list.sort(Comparator.comparing(Feature::getName, c)
			.thenComparing(Feature::getSearchTags, c)
			.thenComparing(Feature::getDescription, c));
	}
	
	public long getPreference(String feature)
	{
		Long preference = preferences.get(feature);
		if(preference == null)
			preference = 0L;
		return preference;
	}
	
	public void addPreference(String feature)
	{
		Long preference = preferences.get(feature);
		if(preference == null)
			preference = 0L;
		preference++;
		preferences.put(feature, preference);
		preferencesFile.save();
	}
	
	public List<Feature> getList()
	{
		return Collections.unmodifiableList(navigatorList);
	}
	
	public void sortFeatures()
	{
		navigatorList.sort(
			Comparator.comparingLong((Feature f) -> getPreference(f.getName()))
				.reversed());
	}
	
	public int countAllFeatures()
	{
		return navigatorList.size();
	}
}
