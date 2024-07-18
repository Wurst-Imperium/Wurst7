/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.Objects;

import com.google.gson.JsonObject;

public final class CrackedAlt extends Alt
{
	private final String name;
	
	/**
	 * @param name
	 *            The Alt's name. Cannot be null or empty.
	 */
	public CrackedAlt(String name)
	{
		this(name, false);
	}
	
	/**
	 * @param name
	 *            The Alt's name. Cannot be null or empty.
	 * @param favorite
	 *            Whether or not the Alt is marked as a favorite.
	 */
	public CrackedAlt(String name, boolean favorite)
	{
		super(favorite);
		
		if(name == null || name.isEmpty())
			throw new IllegalArgumentException();
		
		this.name = name;
	}
	
	/**
	 * Changes the user's cracked name. Happens instantly, cannot fail and does
	 * not trigger any changes that would need to be saved.
	 */
	@Override
	public void login()
	{
		LoginManager.changeCrackedName(name);
	}
	
	@Override
	public void exportAsJson(JsonObject json)
	{
		JsonObject jsonAlt = new JsonObject();
		jsonAlt.addProperty("starred", isFavorite());
		json.add(name, jsonAlt);
	}
	
	@Override
	public String exportAsTXT()
	{
		return name;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String getDisplayName()
	{
		return name;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(name);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		
		if(!(obj instanceof CrackedAlt))
			return false;
		
		CrackedAlt other = (CrackedAlt)obj;
		return Objects.equals(name, other.name);
	}
}
