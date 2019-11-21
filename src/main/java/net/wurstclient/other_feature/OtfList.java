/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_feature;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.TreeMap;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.wurstclient.other_features.HackListOtf;
import net.wurstclient.other_features.WurstLogoOtf;
import net.wurstclient.other_features.ZoomOtf;

public final class OtfList
{
	// public final BookHackOtf bookHackOtf = new BookHackOtf();
	// public final ChangelogOtf changelogOtf = new ChangelogOtf();
	// public final DisableOtf disableOtf = new DisableOtf();
	public final HackListOtf hackListOtf = new HackListOtf();
	// public final ServerFinderOtf serverFinderOtf = new ServerFinderOtf();
	// public final TabGuiOtf tabGuiOtf = new TabGuiOtf();
	// public final TargetOtf targetOtf = new TargetOtf();
	public final WurstLogoOtf wurstLogoOtf = new WurstLogoOtf();
	// public final YesCheatOtf yesCheatOtf = new YesCheatOtf();
	public final ZoomOtf zoomOtf = new ZoomOtf();
	
	private final TreeMap<String, OtherFeature> otfs =
		new TreeMap<>((o1, o2) -> o1.compareToIgnoreCase(o2));
	
	public OtfList()
	{
		try
		{
			for(Field field : OtfList.class.getDeclaredFields())
			{
				if(!field.getName().endsWith("Otf"))
					continue;
				
				OtherFeature otf = (OtherFeature)field.get(this);
				otfs.put(otf.getName(), otf);
			}
			
		}catch(Exception e)
		{
			String message = "Initializing other Wurst features";
			CrashReport report = CrashReport.create(e, message);
			throw new CrashException(report);
		}
	}
	
	public OtherFeature getOtfByName(String name)
	{
		return otfs.get(name);
	}
	
	public Collection<OtherFeature> getAllOtfs()
	{
		return otfs.values();
	}
	
	public int countOtfs()
	{
		return otfs.size();
	}
}
