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

import net.wurstclient.WurstClient;

public final class MojangAlt extends Alt
{
	private final String email;
	private final String password;
	
	private String name = "";
	
	/**
	 * @param email
	 *            The Alt's email address. Cannot be null or empty.
	 * @param password
	 *            The Alt's password. Cannot be null or empty.
	 */
	public MojangAlt(String email, String password)
	{
		this(email, password, "", false);
	}
	
	/**
	 * @param email
	 *            The Alt's email address. Cannot be null or empty.
	 * @param password
	 *            The Alt's password. Cannot be null or empty.
	 * @param name
	 *            The Alt's name, or an empty String if the name is unknown.
	 *            Cannot be null.
	 * @param favorite
	 *            Whether or not the Alt is marked as a favorite.
	 */
	public MojangAlt(String email, String password, String name,
		boolean favorite)
	{
		super(favorite);
		
		if(email == null || email.isEmpty())
			throw new IllegalArgumentException();
		
		if(password == null || password.isEmpty())
			throw new IllegalArgumentException();
		
		this.email = email;
		this.password = password;
		this.name = Objects.requireNonNull(name);
	}
	
	@Override
	public void login() throws LoginException
	{
		MicrosoftLoginManager.login(email, password);
		name = getNameFromSession();
	}
	
	private String getNameFromSession()
	{
		String name = WurstClient.MC.getSession().getUsername();
		
		if(name == null || name.isEmpty())
			throw new RuntimeException(
				"Login returned " + (name == null ? "null" : "empty")
					+ " username. This shouldn't be possible!");
		
		return name;
	}
	
	@Override
	public void exportAsJson(JsonObject json)
	{
		JsonObject jsonAlt = new JsonObject();
		jsonAlt.addProperty("password", password);
		jsonAlt.addProperty("name", name);
		jsonAlt.addProperty("starred", isFavorite());
		json.add(email, jsonAlt);
	}
	
	@Override
	public String exportAsTXT()
	{
		return email + ":" + password;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public String getDisplayName()
	{
		return name.isEmpty() ? email : name;
	}
	
	/**
	 * @return The Alt's email address. Cannot be null or empty.
	 */
	public String getEmail()
	{
		return email;
	}
	
	/**
	 * @return The Alt's password. Cannot be null or empty.
	 */
	public String getPassword()
	{
		return password;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(email);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		
		if(!(obj instanceof MojangAlt))
			return false;
		
		MojangAlt other = (MojangAlt)obj;
		return Objects.equals(email, other.email);
	}
}
