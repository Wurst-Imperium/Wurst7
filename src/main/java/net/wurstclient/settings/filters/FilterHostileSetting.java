/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PiglinEntity;

public final class FilterHostileSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT = "\n\nThis filter does not"
		+ " affect endermen, non-brute piglins, and zombified piglins.";
	
	public FilterHostileSetting(String description, boolean checked)
	{
		super("Filter hostile mobs", description + EXCEPTIONS_TEXT, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		// never filter out neutral mobs (including piglins)
		if(e instanceof Angerable || e instanceof PiglinEntity)
			return false;
		
		return !(e instanceof Monster);
	}
	
	public static FilterHostileSetting genericCombat(boolean checked)
	{
		return new FilterHostileSetting(
			"Won't attack hostile mobs like zombies and creepers.", checked);
	}
	
	public static FilterHostileSetting genericVision(boolean checked)
	{
		return new FilterHostileSetting(
			"Won't show hostile mobs like zombies and creepers.", checked);
	}
}
