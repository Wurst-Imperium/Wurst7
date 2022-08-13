/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;

public final class FilterEndermenSetting extends EntityFilterCheckbox
{
	public FilterEndermenSetting(String description, boolean checked)
	{
		super("Filter endermen", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof EndermanEntity);
	}
	
	public static FilterEndermenSetting genericCombat(boolean checked)
	{
		return new FilterEndermenSetting("Won't attack endermen.", checked);
	}
}
