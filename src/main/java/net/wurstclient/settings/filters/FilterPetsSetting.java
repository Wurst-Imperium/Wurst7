/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.wurstclient.util.text.WText;

public final class FilterPetsSetting extends EntityFilterCheckbox
{
	public FilterPetsSetting(WText description, boolean checked)
	{
		super("Filter pets", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		// Filled sulfur cubes are balls, not pets, even if bucket-released
		if(e instanceof SulfurCube cube && cube.hasBodyItem())
			return false;
		
		if(e instanceof AgeableMob mob && mob.isBaby() && mob.isAgeLocked())
			return true;
		
		// Tadpoles always report fromBucket() == true, even when wild
		if(e instanceof Tadpole tadpole)
			return tadpole.isAgeLocked();
		
		if(e instanceof Bucketable bucketable && bucketable.fromBucket())
			return true;
		
		if(e instanceof SnowGolem || e instanceof CopperGolem
			|| e instanceof IronGolem golem && golem.isPlayerCreated())
			return true;
		
		if(e instanceof TamableAnimal tamable && tamable.isTame())
			return true;
		
		if(e instanceof AbstractHorse horse && !(horse instanceof Camel)
			&& horse.isTamed())
			return true;
		
		if(e instanceof Mob mob && mob.isSaddled()
			&& (e instanceof Camel || e instanceof Strider || e instanceof Pig))
			return true;
		
		if(e instanceof HappyGhast || e instanceof Allay)
			return true;
		
		if(e instanceof Ocelot ocelot && ocelot.isTrusting())
			return true;
		
		if(e instanceof Fox fox
			&& fox.getTrustedEntities().findAny().isPresent())
			return true;
		
		return false;
	}
	
	public static FilterPetsSetting genericCombat(boolean checked)
	{
		return new FilterPetsSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_pets_combat"),
			checked);
	}
	
	public static FilterPetsSetting genericVision(boolean checked)
	{
		return new FilterPetsSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_pets_vision"),
			checked);
	}
}
