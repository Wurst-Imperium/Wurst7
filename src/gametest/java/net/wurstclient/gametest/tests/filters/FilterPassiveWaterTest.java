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
import net.minecraft.world.entity.EntityTypes;
import net.wurstclient.gametest.tests.EntityFilterTest;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;
import net.wurstclient.settings.filters.FilterPassiveWaterSetting;

public final class FilterPassiveWaterTest extends EntityFilterTest
{
	public FilterPassiveWaterTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing passive water mob filter");
		Supplier<EntityFilter> filter =
			() -> new FilterPassiveWaterSetting("", true);
		
		// Filtered out
		assertFilteredOut("Axolotl", filter,
			() -> spawnEntity(EntityTypes.AXOLOTL));
		assertFilteredOut("Cod", filter, () -> spawnEntity(EntityTypes.COD));
		assertFilteredOut("Dolphin", filter,
			() -> spawnEntity(EntityTypes.DOLPHIN));
		assertFilteredOut("Glow Squid", filter,
			() -> spawnEntity(EntityTypes.GLOW_SQUID));
		assertFilteredOut("Salmon", filter,
			() -> spawnEntity(EntityTypes.SALMON));
		assertFilteredOut("Squid", filter,
			() -> spawnEntity(EntityTypes.SQUID));
		assertFilteredOut("Tadpole", filter,
			() -> spawnEntity(EntityTypes.TADPOLE));
		assertFilteredOut("Tropical Fish", filter,
			() -> spawnEntity(EntityTypes.TROPICAL_FISH));
		
		// Allowed because hostile
		assertAllowed("Drowned (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.DROWNED));
		assertAllowed("Elder Guardian (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.ELDER_GUARDIAN));
		assertAllowed("Guardian (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.GUARDIAN));
		assertAllowed("Pufferfish (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.PUFFERFISH));
		
		// Allowed because neutral
		assertAllowed("Nautilus (neutral mob)", filter,
			() -> spawnEntity(EntityTypes.NAUTILUS));
		assertAllowed("Zombie Nautilus (neutral mob)", filter,
			() -> spawnEntity(EntityTypes.ZOMBIE_NAUTILUS));
		
		// Allowed because land-based
		assertAllowed("Silverfish (land-based hostile mob)", filter,
			() -> spawnEntity(EntityTypes.SILVERFISH));
		assertAllowed("Turtle (land-based mob)", filter,
			() -> spawnEntity(EntityTypes.TURTLE));
	}
}
