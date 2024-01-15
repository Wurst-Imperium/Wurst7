/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;

public final class FilterShulkerBulletSetting extends EntityFilterCheckbox
{
	public FilterShulkerBulletSetting(String description, boolean checked)
	{
		super("Filter shulker bullets", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof ShulkerBulletEntity);
	}
	
	public static FilterShulkerBulletSetting genericCombat(boolean checked)
	{
		return new FilterShulkerBulletSetting("Won't attack shulker bullets.",
			checked);
	}
}
