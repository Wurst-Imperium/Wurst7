/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.PufferfishEntity;

public final class FilterNeutralSetting extends AttackDetectingEntityFilter
{
	private FilterNeutralSetting(String description, Mode selected,
		boolean checked)
	{
		super("Filter neutral mobs", description, selected, checked);
	}
	
	public FilterNeutralSetting(String description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	public boolean onTest(Entity e)
	{
		return !(e instanceof Angerable || e instanceof PufferfishEntity
			|| e instanceof PiglinEntity);
	}
	
	@Override
	public boolean ifCalmTest(Entity e)
	{
		// special case for pufferfish
		if(e instanceof PufferfishEntity pfe)
			return pfe.getPuffState() > 0;
		
		if(e instanceof Angerable || e instanceof PiglinEntity)
			if(e instanceof MobEntity me)
				return me.isAttacking();
			
		return true;
	}
	
	public static FilterNeutralSetting genericCombat(Mode selected)
	{
		return new FilterNeutralSetting(
			"description.wurst.setting.generic.filter_neutral_combat",
			selected);
	}
	
	public static FilterNeutralSetting genericVision(Mode selected)
	{
		return new FilterNeutralSetting(
			"description.wurst.setting.generic.filter_neutral_vision",
			selected);
	}
	
	public static FilterNeutralSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterNeutralSetting(description, null, onByDefault);
	}
}
