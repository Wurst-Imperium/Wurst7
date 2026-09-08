/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
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
		// never filter out hostile mobs (including hoglins)
		if(e instanceof Enemy)
			return false;
		
		// never filter out neutral mobs (including pufferfish)
		if(e instanceof NeutralMob || e instanceof Pufferfish)
			return false;
		
		return e instanceof Animal || e instanceof AmbientCreature
			|| e instanceof WaterAnimal || e instanceof AgeableWaterCreature;
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
