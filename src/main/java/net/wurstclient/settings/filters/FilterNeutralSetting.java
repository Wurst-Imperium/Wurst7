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
		return new FilterNeutralSetting("When set to \u00a7lOn\u00a7r,"
			+ " neutral mobs won't be attacked at all.\n\n"
			+ "When set to \u00a7lIf calm\u00a7r, neutral mobs won't be"
			+ " attacked until they attack first. Be warned that this filter"
			+ " cannot detect if the neutral mobs are attacking you or someone"
			+ " else.\n\n"
			+ "When set to \u00a7lOff\u00a7r, this filter does nothing and"
			+ " neutral mobs can be attacked.", selected);
	}
	
	public static FilterNeutralSetting genericVision(Mode selected)
	{
		return new FilterNeutralSetting("When set to \u00a7lOn\u00a7r,"
			+ " neutral mobs won't be shown at all.\n\n"
			+ "When set to \u00a7lIf calm\u00a7r, neutral mobs won't be shown"
			+ " until they attack something.\n\n"
			+ "When set to \u00a7lOff\u00a7r, this filter does nothing and"
			+ " neutral mobs can be shown.", selected);
	}
	
	public static FilterNeutralSetting onOffOnly(String description,
		boolean onByDefault)
	{
		return new FilterNeutralSetting(description, null, onByDefault);
	}
}
