/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieVillagerEntity;

public final class FilterZombieVillagersSetting extends EntityFilterCheckbox
{
	public FilterZombieVillagersSetting(String description, boolean checked)
	{
		super("Filter zombie villagers", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof ZombieVillagerEntity);
	}
	
	public static FilterZombieVillagersSetting genericCombat(boolean checked)
	{
		return new FilterZombieVillagersSetting(
			"Won't attack zombified villagers.", checked);
	}
	
	public static FilterZombieVillagersSetting genericVision(boolean checked)
	{
		return new FilterZombieVillagersSetting(
			"Won't show zombified villagers.", checked);
	}
}
