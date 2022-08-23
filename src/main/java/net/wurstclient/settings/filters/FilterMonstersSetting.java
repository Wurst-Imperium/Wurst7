/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;

public final class FilterMonstersSetting extends EntityFilterCheckbox
{
	public FilterMonstersSetting(String description, boolean checked)
	{
		super("Filter monsters", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof Monster);
	}
	
	public static FilterMonstersSetting genericCombat(boolean checked)
	{
		return new FilterMonstersSetting("Won't attack zombies, creepers, etc.",
			checked);
	}
}
