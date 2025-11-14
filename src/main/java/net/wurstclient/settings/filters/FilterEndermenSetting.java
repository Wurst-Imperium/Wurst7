/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;

public final class FilterEndermenSetting extends AttackDetectingEntityFilter
{
	private FilterEndermenSetting(String description, Mode selected,
		boolean checked)
	{
		super("Filter endermen", description, selected, checked);
	}
	
	public FilterEndermenSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof EnderMan);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		return !(e instanceof EnderMan ee) || ee.isAggressive();
	}
	
	public static FilterEndermenSetting genericCombat(Mode selected)
	{
		return new FilterEndermenSetting(
			"description.wurst.setting.generic.filter_endermen_combat",
			selected);
	}
	
	public static FilterEndermenSetting genericVision(Mode selected)
	{
		return new FilterEndermenSetting(
			"description.wurst.setting.generic.filter_endermen_vision",
			selected);
	}
	
	public static FilterEndermenSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterEndermenSetting(description, null, onByDefault);
	}
}
