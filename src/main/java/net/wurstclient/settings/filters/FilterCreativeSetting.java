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
import net.wurstclient.util.text.WText;

public final class FilterCreativeSetting extends EntityFilterCheckbox
{
	public FilterCreativeSetting(WText description, boolean checked)
	{
		super("Filter creative players", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		return e instanceof Player player && player.isCreative();
	}
	
	public static FilterCreativeSetting genericCombat(boolean checked)
	{
		return new FilterCreativeSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_creative_combat"),
			checked);
	}
	
	public static FilterCreativeSetting genericVision(boolean checked)
	{
		return new FilterCreativeSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_creative_vision"),
			checked);
	}
}
