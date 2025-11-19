/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

public final class FilterVillagersSetting extends EntityFilterCheckbox
{
	public FilterVillagersSetting(String description, boolean checked)
	{
		super("Filter villagers", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof AbstractVillager);
	}
	
	public static FilterVillagersSetting genericCombat(boolean checked)
	{
		return new FilterVillagersSetting(
			"description.wurst.setting.generic.filter_villagers_combat",
			checked);
	}
	
	public static FilterVillagersSetting genericVision(boolean checked)
	{
		return new FilterVillagersSetting(
			"description.wurst.setting.generic.filter_villagers_vision",
			checked);
	}
}
