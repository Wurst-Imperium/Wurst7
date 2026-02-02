/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;

public final class FilterInvisibleSetting extends EntityFilterCheckbox
{
	public FilterInvisibleSetting(String description, boolean checked)
	{
		super("Filter invisible", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !e.isInvisible();
	}
	
	public static FilterInvisibleSetting genericCombat(boolean checked)
	{
		return new FilterInvisibleSetting(
			"description.wurst.setting.generic.filter_invisible_combat",
			checked);
	}
	
	public static FilterInvisibleSetting genericVision(boolean checked)
	{
		return new FilterInvisibleSetting(
			"description.wurst.setting.generic.filter_invisible_vision",
			checked);
	}
}
