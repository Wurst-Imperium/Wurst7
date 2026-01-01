/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public final class FilterPetsSetting extends EntityFilterCheckbox
{
	public FilterPetsSetting(String description, boolean checked)
	{
		super("Filter pets", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return !(e instanceof TamableAnimal && ((TamableAnimal)e).isTame())
			&& !(e instanceof AbstractHorse && ((AbstractHorse)e).isTamed());
	}
	
	public static FilterPetsSetting genericCombat(boolean checked)
	{
		return new FilterPetsSetting(
			"description.wurst.setting.generic.filter_pets_combat", checked);
	}
	
	public static FilterPetsSetting genericVision(boolean checked)
	{
		return new FilterPetsSetting(
			"description.wurst.setting.generic.filter_pets_vision", checked);
	}
}
