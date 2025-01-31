/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;

public final class FilterZombiePiglinsSetting
	extends AttackDetectingEntityFilter
{
	private FilterZombiePiglinsSetting(String description, Mode selected,
		boolean checked)
	{
		super("Filter zombie piglins", description, selected, checked);
	}
	
	public FilterZombiePiglinsSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof ZombifiedPiglinEntity);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		return !(e instanceof ZombifiedPiglinEntity zpe) || zpe.isAttacking();
	}
	
	public static FilterZombiePiglinsSetting genericCombat(Mode selected)
	{
		return new FilterZombiePiglinsSetting(
			"description.wurst.setting.generic.filter_zombie_piglins_combat",
			selected);
	}
	
	public static FilterZombiePiglinsSetting genericVision(Mode selected)
	{
		return new FilterZombiePiglinsSetting(
			"description.wurst.setting.generic.filter_zombie_piglins_vision",
			selected);
	}
	
	public static FilterZombiePiglinsSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterZombiePiglinsSetting(description, null, onByDefault);
	}
}
