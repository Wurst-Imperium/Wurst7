/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;

public final class FilterAnimalsSetting extends EntityFilterCheckbox
{
	public FilterAnimalsSetting(String description, boolean checked)
	{
		super("Filter animals", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof AnimalEntity || e instanceof AmbientEntity
			|| e instanceof WaterCreatureEntity);
	}
	
	public static FilterAnimalsSetting genericCombat(boolean checked)
	{
		return new FilterAnimalsSetting("Won't attack pigs, cows, etc.",
			checked);
	}
}
