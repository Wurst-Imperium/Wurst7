/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.wurstclient.util.text.WText;

public final class FilterPiglinsSetting extends AttackDetectingEntityFilter
{
	private FilterPiglinsSetting(WText description, Mode selected,
		boolean checked)
	{
		super("Filter piglins", description, selected, checked);
	}
	
	public FilterPiglinsSetting(WText description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	protected boolean onFiltersOut(Entity e)
	{
		return e instanceof Piglin;
	}
	
	@Override
	protected boolean ifCalmFiltersOut(Entity e)
	{
		return e instanceof Piglin pe && !pe.isAggressive();
	}
	
	public static FilterPiglinsSetting genericCombat(Mode selected)
	{
		return new FilterPiglinsSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_piglins_combat"),
			selected);
	}
	
	public static FilterPiglinsSetting genericVision(Mode selected)
	{
		return new FilterPiglinsSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_piglins_vision"),
			selected);
	}
	
	public static FilterPiglinsSetting onOffOnly(WText description,
		boolean onByDefault)
	{
		return new FilterPiglinsSetting(description, null, onByDefault);
	}
}
