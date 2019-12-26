/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class AltManager
{
	private final AltsFile altsFile;
	private ArrayList<Alt> alts = new ArrayList<>();
	private int numPremium;
	private int numCracked;
	
	public AltManager(Path altsFile, Path encFolder)
	{
		this.altsFile = new AltsFile(altsFile, encFolder);
		this.altsFile.load(this);
	}
	
	public void addAlt(String email, String password, boolean starred)
	{
		add(new Alt(email, password, null, starred));
	}
	
	private void add(Alt alt)
	{
		alts.add(alt);
		sortAlts();
		altsFile.save(this);
	}
	
	public void editAlt(Alt alt, String newEmail, String newPassword)
	{
		remove(alt);
		add(new Alt(newEmail, newPassword, null, alt.isStarred()));
	}
	
	private void remove(Alt alt)
	{
		if(alts.remove(alt))
			if(alt.isCracked())
				numCracked--;
			else
				numPremium--;
	}
	
	private void sortAlts()
	{
		alts = alts.stream().distinct().sorted()
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		numCracked = (int)alts.stream().filter(Alt::isCracked).count();
		numPremium = alts.size() - numCracked;
	}
	
	public void loadAlts(Collection<? extends Alt> c)
	{
		alts.addAll(c);
		sortAlts();
	}
	
	public List<Alt> getList()
	{
		return Collections.unmodifiableList(alts);
	}
	
	public int getNumPremium()
	{
		return numPremium;
	}
	
	public int getNumCracked()
	{
		return numCracked;
	}
}
