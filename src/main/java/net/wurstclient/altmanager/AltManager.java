/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class AltManager
{
	private final AltsFile altsFile;
	private final ArrayList<Alt> alts = new ArrayList<>();
	private int numPremium;
	private int numCracked;
	
	public AltManager(Path altsFile, Path encFolder)
	{
		this.altsFile = new AltsFile(altsFile, encFolder);
		this.altsFile.load(this);
	}
	
	public boolean contains(String name)
	{
		for(Alt alt : alts)
			if(alt.getName().equalsIgnoreCase(name))
				return true;
			
		return false;
	}
	
	public void add(Alt alt)
	{
		alts.add(alt);
		sortAlts();
		altsFile.save(this);
	}
	
	public void addAll(Collection<Alt> c)
	{
		alts.addAll(c);
		sortAlts();
		altsFile.save(this);
	}
	
	public void edit(Alt oldAlt, String newNameOrEmail, String newPassword)
	{
		remove(oldAlt);
		
		if(newPassword.isEmpty())
			add(new CrackedAlt(newNameOrEmail, oldAlt.isFavorite()));
		else
			add(new MojangAlt(newNameOrEmail, newPassword, "",
				oldAlt.isFavorite()));
	}
	
	/**
	 * Logs the user in with this Alt. Also updates the counter for checked alts
	 * and saves the alt list file as necessary.
	 *
	 * @param alt
	 *            The Alt to login with.
	 * @throws LoginException
	 *             if the login attempt failed for any reason. The reason will
	 *             be explained in the Exception's message, which should be
	 *             displayed to the user.
	 */
	public void login(Alt alt) throws LoginException
	{
		boolean wasUnchecked = alt.isUncheckedPremium();
		
		alt.login();
		
		if(wasUnchecked)
			numPremium++;
		
		if(!alt.isCracked())
			altsFile.save(this);
	}
	
	/**
	 * Changes whether or not the Alt is marked as a favorite, then sorts the
	 * alt list accordingly and saves the changes.
	 */
	public void toggleFavorite(Alt alt)
	{
		alt.setFavorite(!alt.isFavorite());
		sortAlts();
		altsFile.save(this);
	}
	
	/**
	 * Removes the Alt at the given index. Faster than {@link #remove(Alt)}.
	 *
	 * @param index
	 *            The index of the Alt to be removed.
	 * @throws IndexOutOfBoundsException
	 *             if the index is not valid.
	 */
	public void remove(int index)
	{
		Alt alt = alts.get(index);
		alts.remove(index);
		
		if(alt.isCracked())
			numCracked--;
		else if(alt.isCheckedPremium())
			numPremium--;
		
		altsFile.save(this);
	}
	
	/**
	 * Removes the given Alt. Slower than {@link #remove(int)}. Fails safely and
	 * silently if the given Alt is not in the list.
	 *
	 * @param alt
	 *            The Alt to be removed.
	 */
	private void remove(Alt alt)
	{
		if(!alts.remove(alt))
			return;
		
		if(alt.isCracked())
			numCracked--;
		else if(alt.isCheckedPremium())
			numPremium--;
		
		altsFile.save(this);
	}
	
	private void sortAlts()
	{
		Comparator<Alt> c = Comparator.comparing(a -> !a.isFavorite());
		c = c.thenComparing(Alt::isCracked);
		c = c.thenComparing(a -> a.getDisplayName().toLowerCase());
		
		ArrayList<Alt> newAlts = alts.stream().distinct().sorted(c)
			.collect(Collectors.toCollection(ArrayList::new));
		
		alts.clear();
		alts.addAll(newAlts);
		
		numCracked = (int)alts.stream().filter(Alt::isCracked).count();
		numPremium = (int)alts.stream().filter(Alt::isCheckedPremium).count();
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
	
	public Exception getFolderException()
	{
		return altsFile.getFolderException();
	}
}
