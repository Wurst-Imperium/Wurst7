/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ShulkerEntity;

public final class FilterShulkersSetting extends EntityFilterCheckbox
{
	public FilterShulkersSetting(String description, boolean checked)
	{
		super("Filter shulkers", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof ShulkerEntity);
	}
	
	public static FilterShulkersSetting genericCombat(boolean checked)
	{
		return new FilterShulkersSetting("Won't attack shulkers.", checked);
	}
	
	public static FilterShulkersSetting genericVision(boolean checked)
	{
		return new FilterShulkersSetting("Won't show shulkers.", checked);
	}
}
