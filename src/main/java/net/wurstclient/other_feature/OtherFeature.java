/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_feature;

import java.util.Objects;

import net.wurstclient.Feature;

public abstract class OtherFeature extends Feature
{
	private final String name;
	private final String description;
	
	public OtherFeature(String name, String description)
	{
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
		
		if(name.contains(" "))
			throw new IllegalArgumentException(
				"Feature name must not contain spaces: " + name);
	}
	
	@Override
	public final String getName()
	{
		return name;
	}
	
	@Override
	public String getDescription()
	{
		return WURST.translate(description);
	}
	
	@Override
	public boolean isEnabled()
	{
		return false;
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "";
	}
	
	@Override
	public void doPrimaryAction()
	{
		
	}
}
