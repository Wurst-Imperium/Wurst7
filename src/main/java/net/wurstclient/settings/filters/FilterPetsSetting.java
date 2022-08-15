/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.TameableEntity;

public final class FilterPetsSetting extends EntityFilterCheckbox
{
	public FilterPetsSetting(String description, boolean checked)
	{
		super("Filter pets", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof TameableEntity && ((TameableEntity)e).isTamed())
			&& !(e instanceof HorseBaseEntity && ((HorseBaseEntity)e).isTame());
	}
	
	public static FilterPetsSetting genericCombat(boolean checked)
	{
		return new FilterPetsSetting(
			"Won't attack tamed wolves, tamed horses, etc.", checked);
	}
}
