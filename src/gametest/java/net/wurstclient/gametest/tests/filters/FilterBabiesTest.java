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
		Supplier<EntityFilter> filter = () -> new FilterBabiesSetting("", true);
		
		// Passive mobs (filter out if baby)
		for(EntityType<? extends Mob> type : List.of(EntityTypes.ARMADILLO,
			EntityTypes.AXOLOTL, EntityTypes.BEE, EntityTypes.CAMEL,
			EntityTypes.CAT, EntityTypes.CHICKEN, EntityTypes.COW,
			EntityTypes.DOLPHIN, EntityTypes.DONKEY, EntityTypes.FOX,
			EntityTypes.HAPPY_GHAST, EntityTypes.GLOW_SQUID, EntityTypes.GOAT,
			EntityTypes.HORSE, EntityTypes.LLAMA, EntityTypes.MOOSHROOM,
			EntityTypes.MULE, EntityTypes.NAUTILUS, EntityTypes.OCELOT,
			EntityTypes.PANDA, EntityTypes.PIG, EntityTypes.POLAR_BEAR,
			EntityTypes.RABBIT, EntityTypes.SHEEP, EntityTypes.SKELETON_HORSE,
			EntityTypes.SNIFFER, EntityTypes.SQUID, EntityTypes.STRIDER,
			EntityTypes.TRADER_LLAMA, EntityTypes.TURTLE, EntityTypes.WOLF,
			EntityTypes.VILLAGER, EntityTypes.ZOMBIE_HORSE))
		{
			assertFilteredOut(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
			assertAllowed(type.toShortString() + " (adult)", filter,
				() -> spawnEntity(type));
		}
		
		// Special case: Tadpoles (baby frogs) are a separate entity type
		assertFilteredOut(EntityTypes.TADPOLE.toShortString(), filter,
			() -> spawnEntity(EntityTypes.TADPOLE));
		assertAllowed(EntityTypes.FROG.toShortString(), filter,
			() -> spawnEntity(EntityTypes.FROG));
		
		// Hostile mobs (always allow)
		for(EntityType<? extends Mob> type : List.of(EntityTypes.DROWNED,
			EntityTypes.HOGLIN, EntityTypes.HUSK, EntityTypes.ZOGLIN,
			EntityTypes.ZOMBIE, EntityTypes.ZOMBIE_VILLAGER))
		{
			assertAllowed(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
			assertAllowed(type.toShortString() + " (adult)", filter,
				() -> spawnEntity(type));
		}
		
		// Neutral mobs (always allow)
		for(EntityType<? extends Mob> type : List.of(EntityTypes.PIGLIN,
			EntityTypes.ZOMBIFIED_PIGLIN))
		{
			assertAllowed(type.toShortString() + " (baby)", filter,
				() -> spawnBaby(type));
			assertAllowed(type.toShortString() + " (adult)", filter,
				() -> spawnEntity(type));
		}
	}
	
	private <T extends Mob> T spawnBaby(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setBaby(true);
		return entity;
	}
}
