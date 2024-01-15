/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;

public final class FilterArmorStandsSetting extends EntityFilterCheckbox
{
	public FilterArmorStandsSetting(String description, boolean checked)
	{
		super("Filter armor stands", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof ArmorStandEntity);
	}
	
	public static FilterArmorStandsSetting genericCombat(boolean checked)
	{
		return new FilterArmorStandsSetting("Won't attack armor stands.",
			checked);
	}
	
	public static FilterArmorStandsSetting genericVision(boolean checked)
	{
		return new FilterArmorStandsSetting("Won't show armor stands.",
			checked);
	}
}
