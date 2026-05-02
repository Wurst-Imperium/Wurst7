/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests.filters;

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
		Supplier<EntityFilter> filter = () -> new FilterBabiesSetting("", true);
		
		// Filtered out
		assertFilterResult("Baby Armadillo", filter,
			() -> spawnBaby(EntityType.ARMADILLO), false);
		assertFilterResult("Baby Axolotl", filter,
			() -> spawnBaby(EntityType.AXOLOTL), false);
		assertFilterResult("Baby Bee", filter, () -> spawnBaby(EntityType.BEE),
			false);
		assertFilterResult("Baby Camel", filter,
			() -> spawnBaby(EntityType.CAMEL), false);
		assertFilterResult("Kitten (Baby Cat)", filter,
			() -> spawnBaby(EntityType.CAT), false);
		assertFilterResult("Chick (Baby Chicken)", filter,
			() -> spawnBaby(EntityType.CHICKEN), false);
		assertFilterResult("Baby Cow", filter, () -> spawnBaby(EntityType.COW),
			false);
		assertFilterResult("Baby Dolphin", filter,
			() -> spawnBaby(EntityType.DOLPHIN), false);
		assertFilterResult("Baby Donkey", filter,
			() -> spawnBaby(EntityType.DONKEY), false);
		assertFilterResult("Baby Fox", filter, () -> spawnBaby(EntityType.FOX),
			false);
		assertFilterResult("Tadpole (Baby Frog)", filter,
			() -> spawnEntity(EntityType.TADPOLE), false);
		assertFilterResult("Ghastling (Baby Happy Ghast)", filter,
			() -> spawnBaby(EntityType.HAPPY_GHAST), false);
		assertFilterResult("Baby Glow Squid", filter,
			() -> spawnBaby(EntityType.GLOW_SQUID), false);
		assertFilterResult("Baby Goat", filter,
			() -> spawnBaby(EntityType.GOAT), false);
		assertFilterResult("Foal (Baby Horse)", filter,
			() -> spawnBaby(EntityType.HORSE), false);
		assertFilterResult("Baby Llama", filter,
			() -> spawnBaby(EntityType.LLAMA), false);
		assertFilterResult("Baby Mooshroom", filter,
			() -> spawnBaby(EntityType.MOOSHROOM), false);
		assertFilterResult("Baby Mule", filter,
			() -> spawnBaby(EntityType.MULE), false);
		assertFilterResult("Baby Nautilus", filter,
			() -> spawnBaby(EntityType.NAUTILUS), false);
		assertFilterResult("Baby Ocelot", filter,
			() -> spawnBaby(EntityType.OCELOT), false);
		assertFilterResult("Baby Panda", filter,
			() -> spawnBaby(EntityType.PANDA), false);
		assertFilterResult("Baby Pig", filter, () -> spawnBaby(EntityType.PIG),
			false);
		assertFilterResult("Baby Polar Bear", filter,
			() -> spawnBaby(EntityType.POLAR_BEAR), false);
		assertFilterResult("Baby Rabbit", filter,
			() -> spawnBaby(EntityType.RABBIT), false);
		assertFilterResult("Baby Sheep", filter,
			() -> spawnBaby(EntityType.SHEEP), false);
		assertFilterResult("Baby Skeleton Horse", filter,
			() -> spawnBaby(EntityType.SKELETON_HORSE), false);
		assertFilterResult("Snifflet (Baby Sniffer)", filter,
			() -> spawnBaby(EntityType.SNIFFER), false);
		assertFilterResult("Baby Squid", filter,
			() -> spawnBaby(EntityType.SQUID), false);
		assertFilterResult("Baby Strider", filter,
			() -> spawnBaby(EntityType.STRIDER), false);
		assertFilterResult("Baby Trader Llama", filter,
			() -> spawnBaby(EntityType.TRADER_LLAMA), false);
		assertFilterResult("Baby Turtle", filter,
			() -> spawnBaby(EntityType.TURTLE), false);
		assertFilterResult("Puppy (Baby Wolf)", filter,
			() -> spawnBaby(EntityType.WOLF), false);
		assertFilterResult("Baby Villager", filter,
			() -> spawnBaby(EntityType.VILLAGER), false);
		assertFilterResult("Baby Zombie Horse", filter,
			() -> spawnBaby(EntityType.ZOMBIE_HORSE), false);
		
		// Allowed because hostile
		assertFilterResult("Gurgle (Baby Drowned)", filter,
			() -> spawnBaby(EntityType.DROWNED), true);
		assertFilterResult("Baby Hoglin", filter,
			() -> spawnBaby(EntityType.HOGLIN), true);
		assertFilterResult("Baby Husk", filter,
			() -> spawnBaby(EntityType.HUSK), true);
		assertFilterResult("Baby Zoglin", filter,
			() -> spawnBaby(EntityType.ZOGLIN), true);
		assertFilterResult("Baby Zombie", filter,
			() -> spawnBaby(EntityType.ZOMBIE), true);
		assertFilterResult("Baby Zombie Villager", filter,
			() -> spawnBaby(EntityType.ZOMBIE_VILLAGER), true);
		
		// Allowed because neutral
		assertFilterResult("Baby Piglin", filter,
			() -> spawnBaby(EntityType.PIGLIN), true);
		assertFilterResult("Baby Zombified Piglin", filter,
			() -> spawnBaby(EntityType.ZOMBIFIED_PIGLIN), true);
	}
	
	private <T extends Mob> T spawnBaby(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setBaby(true);
		return entity;
	}
}
