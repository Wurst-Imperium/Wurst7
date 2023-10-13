/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PassiveEntity;

public final class FilterBabiesSetting extends EntityFilterCheckbox
{
	public FilterBabiesSetting(String description, boolean checked)
	{
		super("Filter babies", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof PassiveEntity && ((PassiveEntity)e).isBaby());
	}
	
	public static FilterBabiesSetting genericCombat(boolean checked)
	{
		return new FilterBabiesSetting(
			"Won't attack baby pigs, baby villagers, etc.", checked);
	}
}
