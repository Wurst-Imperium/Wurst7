/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.wurstclient.util.text.WText;

public final class FilterZombiePiglinsSetting
	extends AttackDetectingEntityFilter
{
	private FilterZombiePiglinsSetting(WText description, Mode selected,
		boolean checked)
	{
		super("Filter zombie piglins", description, selected, checked);
	}
	
	public FilterZombiePiglinsSetting(WText description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	protected boolean onFiltersOut(Entity e)
	{
		return e instanceof ZombifiedPiglin;
	}
	
	@Override
	protected boolean ifCalmFiltersOut(Entity e)
	{
		return e instanceof ZombifiedPiglin piglin && !(piglin.isAggressive()
			|| piglin.getAttributes().hasModifier(Attributes.MOVEMENT_SPEED,
				ZombifiedPiglin.SPEED_MODIFIER_ATTACKING_ID));
	}
	
	public static FilterZombiePiglinsSetting genericCombat(Mode selected)
	{
		return new FilterZombiePiglinsSetting(WText.translated(
			"description.wurst.setting.generic.filter_zombie_piglins_combat"),
			selected);
	}
	
	public static FilterZombiePiglinsSetting genericVision(Mode selected)
	{
		return new FilterZombiePiglinsSetting(WText.translated(
			"description.wurst.setting.generic.filter_zombie_piglins_vision"),
			selected);
	}
	
	public static FilterZombiePiglinsSetting onOffOnly(WText description,
		boolean onByDefault)
	{
		return new FilterZombiePiglinsSetting(description, null, onByDefault);
	}
}
