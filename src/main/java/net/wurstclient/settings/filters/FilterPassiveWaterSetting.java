/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AxolotlEntity;
import net.minecraft.entity.passive.PufferfishEntity;

public final class FilterPassiveWaterSetting extends EntityFilterCheckbox
{
	private static final String EXCEPTIONS_TEXT =
		"\n\nThis filter does not affect guardians, drowned, and pufferfish.";
	
	public FilterPassiveWaterSetting(String description, boolean checked)
	{
		super("Filter passive water mobs", description + EXCEPTIONS_TEXT,
			checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		// never filter out pufferfish
		if(e instanceof PufferfishEntity)
			return true;
		
		return !(e instanceof WaterCreatureEntity
			|| e instanceof AxolotlEntity);
	}
	
	public static FilterPassiveWaterSetting genericCombat(boolean checked)
	{
		return new FilterPassiveWaterSetting("Won't attack passive water mobs"
			+ " like fish, squid, dolphins and axolotls.", checked);
	}
	
	public static FilterPassiveWaterSetting genericVision(boolean checked)
	{
		return new FilterPassiveWaterSetting("Won't show passive water mobs"
			+ " like fish, squid, dolphins and axolotls.", checked);
	}
}
