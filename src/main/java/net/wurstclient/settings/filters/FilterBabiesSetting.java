/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.monster.Enemy;

public final class FilterBabiesSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT = "\n\nThis filter does not"
		+ " affect baby zombies and other hostile baby mobs.";
	
	public FilterBabiesSetting(String description, boolean checked)
	{
		super("Filter babies", description + EXCEPTIONS_TEXT, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		// never filter out hostile mobs (including hoglins)
		if(e instanceof Enemy)
			return false;
		
		// filter out passive entity babies
		if(e instanceof AgeableMob pe && pe.isBaby())
			return true;
		
		// filter out tadpoles
		if(e instanceof Tadpole)
			return true;
		
		return false;
	}
	
	public static FilterBabiesSetting genericCombat(boolean checked)
	{
		return new FilterBabiesSetting(
			"Won't attack baby pigs, baby villagers, etc.", checked);
	}
}
