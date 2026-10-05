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
import net.minecraft.world.entity.Mob;
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
		assertFilteredOut("baby cow", filter, () -> spawnBaby(EntityType.COW));
		assertAllowed("adult cow", filter, () -> spawnEntity(EntityType.COW));
		
		// Both are hostile, but only hoglins grow up
		assertFilteredOut("baby hoglin", filter,
			() -> spawnBaby(EntityType.HOGLIN));
		assertAllowed("baby zoglin", filter,
			() -> spawnBaby(EntityType.ZOGLIN));
		
		// In 1.21.11, undead horses still use normal AgeableMob aging.
		for(EntityType<? extends Mob> type : List.of(EntityType.HORSE,
			EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE))
			assertFilteredOut(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
		
		// Villagers use the same aging system as animals
		assertFilteredOut("baby villager", filter,
			() -> spawnBaby(EntityType.VILLAGER));
		
		// Tadpoles grow into a separate entity type rather than an adult
		// variant
		assertFilteredOut("tadpole", filter,
			() -> spawnEntity(EntityType.TADPOLE));
		assertAllowed("frog", filter, () -> spawnEntity(EntityType.FROG));
		
		// Natural perma-babies without the AgeableMob aging system
		for(EntityType<? extends Mob> type : List.of(EntityType.PIGLIN,
			EntityType.ZOMBIE))
			assertAllowed(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
		
	}
	
	private <T extends Mob> T spawnBaby(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setBaby(true);
		return entity;
	}
}
