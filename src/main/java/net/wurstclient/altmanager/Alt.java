/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager;

import java.util.Comparator;
import java.util.Objects;

public final class Alt implements Comparable<Alt>
{
	private String email;
	private String name;
	private String password;
	private boolean cracked;
	private boolean unchecked;
	private boolean starred;
	
	public Alt(String email, String password, String name)
	{
		this(email, password, name, false);
	}
	
	public Alt(String email, String password, String name, boolean starred)
	{
		this.email = Objects.requireNonNull(email);
		this.starred = starred;
		
		if(password == null || password.isEmpty())
		{
			cracked = true;
			unchecked = false;
			
			this.name = email;
			this.password = null;
			
		}else
		{
			cracked = false;
			unchecked = name == null || name.isEmpty();
			
			this.name = unchecked ? "" : name;
			this.password = password;
		}
	}
	
	public String getEmail()
	{
		return email;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getNameOrEmail()
	{
		return unchecked ? email : name;
	}
	
	public String getPassword()
	{
		if(password == null || password.isEmpty())
		{
			cracked = true;
			return "";
		}
		
		return password;
	}
	
	public boolean isCracked()
	{
		return cracked;
	}
	
	public boolean isStarred()
	{
		return starred;
	}
	
	public void setStarred(boolean starred)
	{
		this.starred = starred;
	}
	
	public boolean isUnchecked()
	{
		return unchecked;
	}
	
	public void setChecked(String name)
	{
		this.name = name;
		unchecked = false;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Alt))
			return false;
		
		Alt other = (Alt)obj;
		
		return email.equals(other.email) && cracked == other.cracked;
	}
	
	@Override
	public int hashCode()
	{
		return email.hashCode() << 1 | (cracked ? 1 : 0);
	}
	
	@Override
	public int compareTo(Alt o)
	{
		Comparator<Alt> c = Comparator.comparing(a -> !a.starred);
		c = c.thenComparing(a -> a.cracked);
		c = c.thenComparing(a -> a.email.toLowerCase());
		
		return c.compare(this, o);
	}
}
