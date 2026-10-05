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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.fish.Pufferfish;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.wurstclient.util.MobDisposition;
import net.wurstclient.util.text.WText;

public final class FilterNeutralSetting extends AttackDetectingEntityFilter
{
	private FilterNeutralSetting(WText description, Mode selected,
		boolean checked)
	{
		super("Filter neutral mobs", description, selected, checked);
	}
	
	public FilterNeutralSetting(WText description, Mode selected)
	{
		this(description, selected, false);
	}
	
	@Override
	protected boolean onFiltersOut(Entity e)
	{
		return e instanceof Mob mob
			&& MobDisposition.of(mob) == MobDisposition.NEUTRAL;
	}
	
	@Override
	protected boolean ifCalmFiltersOut(Entity e)
	{
		if(!(e instanceof Mob mob)
			|| MobDisposition.of(mob) != MobDisposition.NEUTRAL)
			return false;
		
		if(mob instanceof Wolf wolf)
			return !wolf.isAngry() && !wolf.isAggressive();
		
		if(mob instanceof Bee bee)
			return !bee.isAngry() && !bee.isAggressive();
		
		if(mob instanceof EnderMan enderman)
			return !enderman.isCreepy();
		
		if(mob instanceof Pufferfish pufferfish)
			return pufferfish.getPuffState() <= 0;
		
		// Goats use events, so clients that start tracking mid-ram can miss it.
		if(mob instanceof Goat goat)
			return !goat.isLoweringHead;
		
		// Panda.isAggressive() returns its personality, not its attack flag.
		if(mob instanceof Panda panda)
			return (panda.getEntityData().get(Mob.DATA_MOB_FLAGS_ID) & 4) == 0;
		
		if(mob instanceof ZombifiedPiglin piglin)
			return !(piglin.isAggressive()
				|| piglin.getAttributes().hasModifier(Attributes.MOVEMENT_SPEED,
					ZombifiedPiglin.SPEED_MODIFIER_ATTACKING_ID));
		
		// Neither nautilus type exposes its aggression to the client.
		if(mob instanceof AbstractNautilus)
			return true;
		
		return !mob.isAggressive();
	}
	
	public static FilterNeutralSetting genericCombat(Mode selected)
	{
		return new FilterNeutralSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_neutral_combat"),
			selected);
	}
	
	public static FilterNeutralSetting genericVision(Mode selected)
	{
		return new FilterNeutralSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_neutral_vision"),
			selected);
	}
	
	public static FilterNeutralSetting onOffOnly(WText description,
		boolean onByDefault)
	{
		return new FilterNeutralSetting(description, null, onByDefault);
	}
}
