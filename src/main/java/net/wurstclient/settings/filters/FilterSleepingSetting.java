/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.wurstclient.util.text.WText;

public final class FilterSleepingSetting extends EntityFilterCheckbox
{
	public FilterSleepingSetting(WText description, boolean checked)
	{
		super("Filter sleeping players", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		if(!(e instanceof Avatar pe))
			return false;
		
		return pe.isSleeping() || pe.getPose() == Pose.SLEEPING;
	}
	
	public static FilterSleepingSetting genericCombat(boolean checked)
	{
		return new FilterSleepingSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_sleeping_combat"),
			checked);
	}
	
	public static FilterSleepingSetting genericVision(boolean checked)
	{
		return new FilterSleepingSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_sleeping_vision"),
			checked);
	}
}
