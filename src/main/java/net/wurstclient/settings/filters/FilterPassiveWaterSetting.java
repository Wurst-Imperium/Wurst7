/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.wurstclient.util.text.WText;

public final class FilterPassiveWaterSetting extends EntityFilterCheckbox
{
	public FilterPassiveWaterSetting(WText description, boolean checked)
	{
		super("Filter passive water mobs", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		// never filter out pufferfish
		if(e instanceof Pufferfish)
			return false;
		
		return e instanceof AbstractFish || e instanceof Squid
			|| e instanceof Dolphin || e instanceof Axolotl
			|| e instanceof Turtle;
	}
	
	public static FilterPassiveWaterSetting genericCombat(boolean checked)
	{
		return new FilterPassiveWaterSetting(WText.translated(
			"description.wurst.setting.generic.filter_passive_water_combat"),
			checked);
	}
	
	public static FilterPassiveWaterSetting genericVision(boolean checked)
	{
		return new FilterPassiveWaterSetting(WText.translated(
			"description.wurst.setting.generic.filter_passive_water_vision"),
			checked);
	}
}
