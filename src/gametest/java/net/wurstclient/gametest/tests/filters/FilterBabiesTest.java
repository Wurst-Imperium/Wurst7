/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests.filters;

import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.wurstclient.gametest.tests.EntityFilterTest;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;
import net.wurstclient.settings.filters.FilterBabiesSetting;

public final class FilterBabiesTest extends EntityFilterTest
{
	public FilterBabiesTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing baby mob filter");
		Supplier<EntityFilter> filter =
			() -> FilterBabiesSetting.genericCombat(true);
		
		// Normal baby/adult baseline
		assertFilteredOut("baby cow", filter, () -> spawnBaby(EntityTypes.COW));
		assertAllowed("adult cow", filter, () -> spawnEntity(EntityTypes.COW));
		
		// Both are hostile, but only hoglins grow up
		assertFilteredOut("baby hoglin", filter,
			() -> spawnBaby(EntityTypes.HOGLIN));
		assertAllowed("baby zoglin", filter,
			() -> spawnBaby(EntityTypes.ZOGLIN));
		
		// Undead horses are AgeableMobs, but override canAgeUp() to false
		assertFilteredOut("baby horse", filter,
			() -> spawnBaby(EntityTypes.HORSE));
		for(EntityType<? extends Mob> type : List.of(EntityTypes.SKELETON_HORSE,
			EntityTypes.ZOMBIE_HORSE))
			assertAllowed(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
		
		// Villagers grow up even though they cannot be age-locked
		assertFilteredOut("baby villager", filter,
			() -> spawnBaby(EntityTypes.VILLAGER));
		
		// Tadpoles grow into a separate entity type rather than an adult
		// variant
		assertFilteredOut("tadpole", filter,
			() -> spawnEntity(EntityTypes.TADPOLE));
		assertAllowed("frog", filter, () -> spawnEntity(EntityTypes.FROG));
		
		// Natural perma-babies without the AgeableMob aging system
		for(EntityType<? extends Mob> type : List.of(EntityTypes.PIGLIN,
			EntityTypes.ZOMBIE))
			assertAllowed(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
		
		// Golden dandelions stop aging but must not bypass the filter
		assertFilteredOut("age-locked cow", filter, () -> {
			Cow cow = spawnBaby(EntityTypes.COW);
			cow.setAgeLocked(true);
			return cow;
		});
		assertFilteredOut("age-locked tadpole", filter, () -> {
			Tadpole tadpole = spawnEntity(EntityTypes.TADPOLE);
			tadpole.setAgeLocked(true);
			return tadpole;
		});
	}
	
	private <T extends Mob> T spawnBaby(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setBaby(true);
		return entity;
	}
}
