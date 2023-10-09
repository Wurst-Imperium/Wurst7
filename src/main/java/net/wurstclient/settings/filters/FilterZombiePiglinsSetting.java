/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;

public final class FilterZombiePiglinsSetting extends EntityFilterCheckbox
{
	public FilterZombiePiglinsSetting(String description, boolean checked)
	{
		super("Filter zombie piglins", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof ZombifiedPiglinEntity);
	}
	
	public static FilterZombiePiglinsSetting genericCombat(boolean checked)
	{
		return new FilterZombiePiglinsSetting("Won't attack zombified piglins.",
			checked);
	}
	
	public static FilterZombiePiglinsSetting genericVision(boolean checked)
	{
		return new FilterZombiePiglinsSetting("Won't show zombified piglins.",
			checked);
	}
}
