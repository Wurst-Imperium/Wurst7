/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public final class FilterMinecartsSetting extends EntityFilterCheckbox
{
	public FilterMinecartsSetting(String description, boolean checked)
	{
		super("Filter minecarts", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof AbstractMinecartEntity);
	}
}
