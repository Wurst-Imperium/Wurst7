/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Shulker;

public final class FilterGolemsSetting extends EntityFilterCheckbox
{
	public FilterGolemsSetting(String description, boolean checked)
	{
		super("Filter golems", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof AbstractGolem) || e instanceof Shulker;
	}
	
	public static FilterGolemsSetting genericCombat(boolean checked)
	{
		return new FilterGolemsSetting(
			"description.wurst.setting.generic.filter_golems_combat", checked);
	}
	
	public static FilterGolemsSetting genericVision(boolean checked)
	{
		return new FilterGolemsSetting(
			"description.wurst.setting.generic.filter_golems_vision", checked);
	}
}
