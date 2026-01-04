/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;

public final class FilterSlimesSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT =
		"\n\nThis filter does not affect magma cubes.";
	
	public FilterSlimesSetting(String description, boolean checked)
	{
		super("Filter slimes", description + EXCEPTIONS_TEXT, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof Slime) || e instanceof MagmaCube;
	}
	
	public static FilterSlimesSetting genericCombat(boolean checked)
	{
		return new FilterSlimesSetting("Won't attack slimes.", checked);
	}
	
	public static FilterSlimesSetting genericVision(boolean checked)
	{
		return new FilterSlimesSetting("Won't show slimes.", checked);
	}
}
