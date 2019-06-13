/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import java.util.Objects;

import net.wurstclient.Feature;
import net.wurstclient.WurstClient;

public abstract class Hack extends Feature
{
	protected final WurstClient wurst;
	
	private final String name;
	private final String description;
	private HackCategory category;
	
	// TODO
	// private final LinkedHashMap<String, Setting> settings =
	// new LinkedHashMap<>();
	
	private boolean enabled;
	private final boolean stateSaved =
		!getClass().isAnnotationPresent(DontSaveState.class);
	
	public Hack(WurstClient wurst, String name, String description)
	{
		this.wurst = Objects.requireNonNull(wurst);
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
	}
	
	public final String getName()
	{
		return name;
	}
	
	public String getRenderName()
	{
		return name;
	}
	
	public final String getDescription()
	{
		return description;
	}
	
	public final HackCategory getCategory()
	{
		return category;
	}
	
	protected final void setCategory(HackCategory category)
	{
		this.category = category;
	}
	
	// TODO
	// public final Map<String, Setting> getSettings()
	// {
	// return Collections.unmodifiableMap(settings);
	// }
	//
	// protected final void addSetting(Setting setting)
	// {
	// String key = setting.getName().toLowerCase();
	//
	// if(settings.containsKey(key))
	// throw new IllegalArgumentException(
	// "Duplicate setting: " + name + " " + key);
	//
	// settings.put(key, setting);
	// }
	
	public final boolean isEnabled()
	{
		return enabled;
	}
	
	public final void setEnabled(boolean enabled)
	{
		if(this.enabled == enabled)
			return;
		
		this.enabled = enabled;
		
		if(enabled)
			onEnable();
		else
			onDisable();
		
		if(stateSaved)
			wurst.getHax().saveEnabledHax();
	}
	
	public final boolean isStateSaved()
	{
		return stateSaved;
	}
	
	protected void onEnable()
	{
		
	}
	
	protected void onDisable()
	{
		
	}
}
