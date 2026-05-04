/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;

public final class FilterPetsSetting extends EntityFilterCheckbox
{
	public FilterPetsSetting(String description, boolean checked)
	{
		super("Filter pets", description, checked);
	}
	
	@Override
	public boolean test(Entity e)
	{
		if(e instanceof TamableAnimal tamable && tamable.isTame())
			return false;
		
		if(e instanceof AbstractHorse horse && !(horse instanceof Camel)
			&& horse.isTamed())
			return false;
		
		if(e instanceof Mob mob && isPetIfSaddled(mob) && mob.isSaddled())
			return false;
		
		if(e instanceof HappyGhast)
			return false;
		
		if(e instanceof Ocelot ocelot && ocelot.isTrusting())
			return false;
		
		if(e instanceof Fox fox
			&& fox.getTrustedEntities().findAny().isPresent())
			return false;
		
		return true;
	}
	
	private boolean isPetIfSaddled(Mob e)
	{
		return e instanceof Camel || e instanceof Strider || e instanceof Pig;
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
