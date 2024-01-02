/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.AmbientEntity;

public final class FilterBatsSetting extends EntityFilterCheckbox
{
	public FilterBatsSetting(String description, boolean checked)
	{
		super("Filter bats", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof AmbientEntity);
	}
	
	public static FilterBatsSetting genericCombat(boolean checked)
	{
		return new FilterBatsSetting("Won't attack bats and any other"
			+ " \"ambient\" mobs that might be added by mods.", checked);
	}
	
	public static FilterBatsSetting genericVision(boolean checked)
	{
		return new FilterBatsSetting("Won't show bats and any other"
			+ " \"ambient\" mobs that might be added by mods.", checked);
	}
}
