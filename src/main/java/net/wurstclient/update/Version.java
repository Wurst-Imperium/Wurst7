/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.update;

import java.util.regex.Pattern;

public final class Version implements Comparable<Version>
{
	private static final Pattern SYNTAX =
		Pattern.compile("^[0-9]+\\.[0-9]+(?:\\.[0-9]+)?(?:pre[0-9]+)?$");
	
	private final int major;
	private final int minor;
	private final int patch;
	private final int preRelease;
	
	public Version(String version)
	{
		if(!SYNTAX.asPredicate().test(version))
		{
			major = -1;
			minor = -1;
			patch = -1;
			preRelease = Integer.MAX_VALUE;
			return;
		}
		
		int indexOfPre = version.indexOf("pre");
		
		String[] parts;
		if(indexOfPre == -1)
		{
			preRelease = Integer.MAX_VALUE;
			parts = version.split("\\.");
			
		}else
		{
			preRelease = Integer.parseInt(version.substring(indexOfPre + 3));
			parts = version.substring(0, indexOfPre).split("\\.");
		}
		
		major = Integer.parseInt(parts[0]);
		
		minor = Integer.parseInt(parts[1]);
		
		if(parts.length == 3)
			patch = Integer.parseInt(parts[2]);
		else
			patch = 0;
	}
	
	@Override
	public int hashCode()
	{
		return major << 24 | minor << 16 | patch << 8 | preRelease;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		return super.equals(obj)
			|| obj instanceof Version && compareTo((Version)obj) == 0;
	}
	
	@Override
	public int compareTo(Version o)
	{
		if(major != o.major)
			return Integer.compare(major, o.major);
		
		if(minor != o.minor)
			return Integer.compare(minor, o.minor);
		
		if(patch != o.patch)
			return Integer.compare(patch, o.patch);
		
		if(preRelease != o.preRelease)
			return Integer.compare(preRelease, o.preRelease);
		
		return 0;
	}
	
	public boolean shouldUpdateTo(Version other)
	{
		return isInvalid() || other.isInvalid() || isLowerThan(other);
	}
	
	public boolean isLowerThan(Version other)
	{
		return compareTo(other) < 0;
	}
	
	public boolean isLowerThan(String other)
	{
		return isLowerThan(new Version(other));
	}
	
	public boolean isHigherThan(Version other)
	{
		return compareTo(other) > 0;
	}
	
	public boolean isHigherThan(String other)
	{
		return isHigherThan(new Version(other));
	}
	
	@Override
	public String toString()
	{
		if(isInvalid())
			return "(invalid version)";
		
		String s = major + "." + minor;
		
		if(patch > 0)
			s += "." + patch;
		
		if(isPreRelease())
			s += "pre" + preRelease;
		
		return s;
	}
	
	public boolean isInvalid()
	{
		return major == -1 && minor == -1 && patch == -1;
	}
	
	public boolean isPreRelease()
	{
		return preRelease != Integer.MAX_VALUE;
	}
	
	public String getChangelogLink()
	{
		String version = major + "-" + minor;
		
		if(isPreRelease())
			version += "pre" + preRelease;
		
		return "https://www.wurstclient.net/updates/wurst-" + version + "/";
	}
}
