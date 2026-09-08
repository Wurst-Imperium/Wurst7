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

public final class FilterHostileSetting extends EntityFilterCheckbox
{
	public FilterHostileSetting(WText description, boolean checked)
	{
		super("Filter hostile mobs", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		return e instanceof Mob mob
			&& MobDisposition.of(mob) == MobDisposition.HOSTILE;
	}
	
	public static FilterHostileSetting genericCombat(boolean checked)
	{
		return new FilterHostileSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_hostile_combat"),
			checked);
	}
	
	public static FilterHostileSetting genericVision(boolean checked)
	{
		return new FilterHostileSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_hostile_vision"),
			checked);
	}
}
