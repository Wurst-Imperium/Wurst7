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
		
		// Filtered out
		assertFilterResult("Baby Armadillo", filter,
			() -> spawnBaby(EntityTypes.ARMADILLO), false);
		assertFilterResult("Baby Axolotl", filter,
			() -> spawnBaby(EntityTypes.AXOLOTL), false);
		assertFilterResult("Baby Bee", filter, () -> spawnBaby(EntityTypes.BEE),
			false);
		assertFilterResult("Baby Camel", filter,
			() -> spawnBaby(EntityTypes.CAMEL), false);
		assertFilterResult("Kitten (Baby Cat)", filter,
			() -> spawnBaby(EntityTypes.CAT), false);
		assertFilterResult("Chick (Baby Chicken)", filter,
			() -> spawnBaby(EntityTypes.CHICKEN), false);
		assertFilterResult("Baby Cow", filter, () -> spawnBaby(EntityTypes.COW),
			false);
		assertFilterResult("Baby Dolphin", filter,
			() -> spawnBaby(EntityTypes.DOLPHIN), false);
		assertFilterResult("Baby Donkey", filter,
			() -> spawnBaby(EntityTypes.DONKEY), false);
		assertFilterResult("Baby Fox", filter, () -> spawnBaby(EntityTypes.FOX),
			false);
		assertFilterResult("Tadpole (Baby Frog)", filter,
			() -> spawnEntity(EntityTypes.TADPOLE), false);
		assertFilterResult("Ghastling (Baby Happy Ghast)", filter,
			() -> spawnBaby(EntityTypes.HAPPY_GHAST), false);
		assertFilterResult("Baby Glow Squid", filter,
			() -> spawnBaby(EntityTypes.GLOW_SQUID), false);
		assertFilterResult("Baby Goat", filter,
			() -> spawnBaby(EntityTypes.GOAT), false);
		assertFilterResult("Foal (Baby Horse)", filter,
			() -> spawnBaby(EntityTypes.HORSE), false);
		assertFilterResult("Baby Llama", filter,
			() -> spawnBaby(EntityTypes.LLAMA), false);
		assertFilterResult("Baby Mooshroom", filter,
			() -> spawnBaby(EntityTypes.MOOSHROOM), false);
		assertFilterResult("Baby Mule", filter,
			() -> spawnBaby(EntityTypes.MULE), false);
		assertFilterResult("Baby Nautilus", filter,
			() -> spawnBaby(EntityTypes.NAUTILUS), false);
		assertFilterResult("Baby Ocelot", filter,
			() -> spawnBaby(EntityTypes.OCELOT), false);
		assertFilterResult("Baby Panda", filter,
			() -> spawnBaby(EntityTypes.PANDA), false);
		assertFilterResult("Baby Pig", filter, () -> spawnBaby(EntityTypes.PIG),
			false);
		assertFilterResult("Baby Polar Bear", filter,
			() -> spawnBaby(EntityTypes.POLAR_BEAR), false);
		assertFilterResult("Baby Rabbit", filter,
			() -> spawnBaby(EntityTypes.RABBIT), false);
		assertFilterResult("Baby Sheep", filter,
			() -> spawnBaby(EntityTypes.SHEEP), false);
		assertFilterResult("Baby Skeleton Horse", filter,
			() -> spawnBaby(EntityTypes.SKELETON_HORSE), false);
		assertFilterResult("Snifflet (Baby Sniffer)", filter,
			() -> spawnBaby(EntityTypes.SNIFFER), false);
		assertFilterResult("Baby Squid", filter,
			() -> spawnBaby(EntityTypes.SQUID), false);
		assertFilterResult("Baby Strider", filter,
			() -> spawnBaby(EntityTypes.STRIDER), false);
		assertFilterResult("Baby Trader Llama", filter,
			() -> spawnBaby(EntityTypes.TRADER_LLAMA), false);
		assertFilterResult("Baby Turtle", filter,
			() -> spawnBaby(EntityTypes.TURTLE), false);
		assertFilterResult("Puppy (Baby Wolf)", filter,
			() -> spawnBaby(EntityTypes.WOLF), false);
		assertFilterResult("Baby Villager", filter,
			() -> spawnBaby(EntityTypes.VILLAGER), false);
		assertFilterResult("Baby Zombie Horse", filter,
			() -> spawnBaby(EntityTypes.ZOMBIE_HORSE), false);
		
		// Allowed because hostile
		assertFilterResult("Gurgle (Baby Drowned)", filter,
			() -> spawnBaby(EntityTypes.DROWNED), true);
		assertFilterResult("Baby Hoglin", filter,
			() -> spawnBaby(EntityTypes.HOGLIN), true);
		assertFilterResult("Baby Husk", filter,
			() -> spawnBaby(EntityTypes.HUSK), true);
		assertFilterResult("Baby Zoglin", filter,
			() -> spawnBaby(EntityTypes.ZOGLIN), true);
		assertFilterResult("Baby Zombie", filter,
			() -> spawnBaby(EntityTypes.ZOMBIE), true);
		assertFilterResult("Baby Zombie Villager", filter,
			() -> spawnBaby(EntityTypes.ZOMBIE_VILLAGER), true);
		
		// Allowed because neutral
		assertFilterResult("Baby Piglin", filter,
			() -> spawnBaby(EntityTypes.PIGLIN), true);
		assertFilterResult("Baby Zombified Piglin", filter,
			() -> spawnBaby(EntityTypes.ZOMBIFIED_PIGLIN), true);
	}
	
	private <T extends Mob> T spawnBaby(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setBaby(true);
		return entity;
	}
}
