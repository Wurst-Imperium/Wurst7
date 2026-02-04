/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class FilterPlayersSetting extends EntityFilterCheckbox
{
	public FilterPlayersSetting(String description, boolean checked)
	{
		super("Filter players", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof Player);
	}
	
	public static FilterPlayersSetting genericCombat(boolean checked)
	{
		return new FilterPlayersSetting(
			"description.wurst.setting.generic.filter_players_combat", checked);
	}
	
	public static FilterPlayersSetting genericVision(boolean checked)
	{
		return new FilterPlayersSetting(
			"description.wurst.setting.generic.filter_players_vision", checked);
	}
}
