/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.wurstclient.util.MobDisposition;
import net.wurstclient.util.text.WText;

public final class FilterPassiveSetting extends EntityFilterCheckbox
{
	public FilterPassiveSetting(WText description, boolean checked)
	{
		super("Filter passive mobs", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		return e instanceof Mob mob
			&& MobDisposition.of(mob) == MobDisposition.PASSIVE;
	}
	
	public static FilterPassiveSetting genericCombat(boolean checked)
	{
		return new FilterPassiveSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_passive_combat"),
			checked);
	}
	
	public static FilterPassiveSetting genericVision(boolean checked)
	{
		return new FilterPassiveSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_passive_vision"),
			checked);
	}
}
