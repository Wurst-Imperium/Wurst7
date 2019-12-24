/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Collectors;

public class AltList implements Iterable<Alt>
{
	private ArrayList<Alt> alts = new ArrayList<>();
	private int numPremium;
	private int numCracked;
	
	private void sortAlts()
	{
		alts = alts.stream().distinct().sorted()
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		numCracked = (int)alts.stream().filter(Alt::isCracked).count();
		numPremium = alts.size() - numCracked;
	}
	
	public int getNumPremium()
	{
		return numPremium;
	}
	
	public int getNumCracked()
	{
		return numCracked;
	}
	
	public void add(Alt alt)
	{
		alts.add(alt);
		sortAlts();
	}
	
	public void addAll(Collection<? extends Alt> c)
	{
		alts.addAll(c);
		sortAlts();
	}
	
	public void remove(Alt alt)
	{
		if(alts.remove(alt))
			if(alt.isCracked())
				numCracked--;
			else
				numPremium--;
	}
	
	@Override
	public Iterator<Alt> iterator()
	{
		return Collections.unmodifiableList(alts).iterator();
	}
}
