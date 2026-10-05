/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.wurstclient.util.text.WText;

public final class FilterProjectilesSetting extends EntityFilterCheckbox
{
	public FilterProjectilesSetting(WText description, boolean checked)
	{
		super("Filter projectiles", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		return (e instanceof Projectile
			&& e.is(EntityTypeTags.REDIRECTABLE_PROJECTILE))
			|| e instanceof ShulkerBullet;
	}
	
	public static FilterProjectilesSetting genericCombat(boolean checked)
	{
		return new FilterProjectilesSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_projectiles_combat"),
			checked);
	}
}
