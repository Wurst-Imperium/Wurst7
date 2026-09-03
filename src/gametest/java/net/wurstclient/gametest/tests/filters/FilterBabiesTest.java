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
		assertFilteredOut("Baby Armadillo", filter,
			() -> spawnBaby(EntityTypes.ARMADILLO));
		assertFilteredOut("Baby Axolotl", filter,
			() -> spawnBaby(EntityTypes.AXOLOTL));
		assertFilteredOut("Baby Bee", filter, () -> spawnBaby(EntityTypes.BEE));
		assertFilteredOut("Baby Camel", filter,
			() -> spawnBaby(EntityTypes.CAMEL));
		assertFilteredOut("Kitten (Baby Cat)", filter,
			() -> spawnBaby(EntityTypes.CAT));
		assertFilteredOut("Chick (Baby Chicken)", filter,
			() -> spawnBaby(EntityTypes.CHICKEN));
		assertFilteredOut("Baby Cow", filter, () -> spawnBaby(EntityTypes.COW));
		assertFilteredOut("Baby Dolphin", filter,
			() -> spawnBaby(EntityTypes.DOLPHIN));
		assertFilteredOut("Baby Donkey", filter,
			() -> spawnBaby(EntityTypes.DONKEY));
		assertFilteredOut("Baby Fox", filter, () -> spawnBaby(EntityTypes.FOX));
		assertFilteredOut("Tadpole (Baby Frog)", filter,
			() -> spawnEntity(EntityTypes.TADPOLE));
		assertFilteredOut("Ghastling (Baby Happy Ghast)", filter,
			() -> spawnBaby(EntityTypes.HAPPY_GHAST));
		assertFilteredOut("Baby Glow Squid", filter,
			() -> spawnBaby(EntityTypes.GLOW_SQUID));
		assertFilteredOut("Baby Goat", filter,
			() -> spawnBaby(EntityTypes.GOAT));
		assertFilteredOut("Foal (Baby Horse)", filter,
			() -> spawnBaby(EntityTypes.HORSE));
		assertFilteredOut("Baby Llama", filter,
			() -> spawnBaby(EntityTypes.LLAMA));
		assertFilteredOut("Baby Mooshroom", filter,
			() -> spawnBaby(EntityTypes.MOOSHROOM));
		assertFilteredOut("Baby Mule", filter,
			() -> spawnBaby(EntityTypes.MULE));
		assertFilteredOut("Baby Nautilus", filter,
			() -> spawnBaby(EntityTypes.NAUTILUS));
		assertFilteredOut("Baby Ocelot", filter,
			() -> spawnBaby(EntityTypes.OCELOT));
		assertFilteredOut("Baby Panda", filter,
			() -> spawnBaby(EntityTypes.PANDA));
		assertFilteredOut("Baby Pig", filter, () -> spawnBaby(EntityTypes.PIG));
		assertFilteredOut("Baby Polar Bear", filter,
			() -> spawnBaby(EntityTypes.POLAR_BEAR));
		assertFilteredOut("Baby Rabbit", filter,
			() -> spawnBaby(EntityTypes.RABBIT));
		assertFilteredOut("Baby Sheep", filter,
			() -> spawnBaby(EntityTypes.SHEEP));
		assertFilteredOut("Baby Skeleton Horse", filter,
			() -> spawnBaby(EntityTypes.SKELETON_HORSE));
		assertFilteredOut("Snifflet (Baby Sniffer)", filter,
			() -> spawnBaby(EntityTypes.SNIFFER));
		assertFilteredOut("Baby Squid", filter,
			() -> spawnBaby(EntityTypes.SQUID));
		assertFilteredOut("Baby Strider", filter,
			() -> spawnBaby(EntityTypes.STRIDER));
		assertFilteredOut("Baby Trader Llama", filter,
			() -> spawnBaby(EntityTypes.TRADER_LLAMA));
		assertFilteredOut("Baby Turtle", filter,
			() -> spawnBaby(EntityTypes.TURTLE));
		assertFilteredOut("Puppy (Baby Wolf)", filter,
			() -> spawnBaby(EntityTypes.WOLF));
		assertFilteredOut("Baby Villager", filter,
			() -> spawnBaby(EntityTypes.VILLAGER));
		assertFilteredOut("Baby Zombie Horse", filter,
			() -> spawnBaby(EntityTypes.ZOMBIE_HORSE));
		
		// Allowed because hostile
		assertAllowed("Gurgle (Baby Drowned)", filter,
			() -> spawnBaby(EntityTypes.DROWNED));
		assertAllowed("Baby Hoglin", filter,
			() -> spawnBaby(EntityTypes.HOGLIN));
		assertAllowed("Baby Husk", filter, () -> spawnBaby(EntityTypes.HUSK));
		assertAllowed("Baby Zoglin", filter,
			() -> spawnBaby(EntityTypes.ZOGLIN));
		assertAllowed("Baby Zombie", filter,
			() -> spawnBaby(EntityTypes.ZOMBIE));
		assertAllowed("Baby Zombie Villager", filter,
			() -> spawnBaby(EntityTypes.ZOMBIE_VILLAGER));
		
		// Allowed because neutral
		assertAllowed("Baby Piglin", filter,
			() -> spawnBaby(EntityTypes.PIGLIN));
		assertAllowed("Baby Zombified Piglin", filter,
			() -> spawnBaby(EntityTypes.ZOMBIFIED_PIGLIN));
	}
	
	private <T extends Mob> T spawnBaby(EntityType<T> type)
	{
		T entity = spawnEntity(type);
		entity.setBaby(true);
		return entity;
	}
}
