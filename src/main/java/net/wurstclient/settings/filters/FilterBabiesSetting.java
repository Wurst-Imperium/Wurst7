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
import net.wurstclient.hack.Hack;
import net.wurstclient.util.text.WText;

public final class FilterBabiesSetting extends EntityFilterCheckbox
{
	public FilterBabiesSetting(WText description, boolean checked)
	{
		super("Filter babies", description, checked);
	}
	
	public FilterBabiesSetting(Hack hack, boolean checked)
	{
		this(WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".filter_babies"), checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		if(e instanceof Tadpole)
			return true;
		
		return e instanceof AgeableMob mob && mob.isBaby()
			&& (mob.canAgeUp() || mob.isAgeLocked());
	}
	
	public static FilterBabiesSetting genericCombat(boolean checked)
	{
		return new FilterBabiesSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_babies_combat"),
			checked);
	}
}
