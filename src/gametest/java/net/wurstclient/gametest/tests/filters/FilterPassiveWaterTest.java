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
		assertFilterResult("Axolotl", filter,
			() -> spawnEntity(EntityTypes.AXOLOTL), false);
		assertFilterResult("Cod", filter, () -> spawnEntity(EntityTypes.COD),
			false);
		assertFilterResult("Dolphin", filter,
			() -> spawnEntity(EntityTypes.DOLPHIN), false);
		assertFilterResult("Glow Squid", filter,
			() -> spawnEntity(EntityTypes.GLOW_SQUID), false);
		assertFilterResult("Salmon", filter,
			() -> spawnEntity(EntityTypes.SALMON), false);
		assertFilterResult("Squid", filter,
			() -> spawnEntity(EntityTypes.SQUID), false);
		assertFilterResult("Tadpole", filter,
			() -> spawnEntity(EntityTypes.TADPOLE), false);
		assertFilterResult("Tropical Fish", filter,
			() -> spawnEntity(EntityTypes.TROPICAL_FISH), false);
		
		// Allowed because hostile
		assertFilterResult("Drowned (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.DROWNED), true);
		assertFilterResult("Elder Guardian (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.ELDER_GUARDIAN), true);
		assertFilterResult("Guardian (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.GUARDIAN), true);
		assertFilterResult("Pufferfish (hostile mob)", filter,
			() -> spawnEntity(EntityTypes.PUFFERFISH), true);
		
		// Allowed because neutral
		assertFilterResult("Nautilus (neutral mob)", filter,
			() -> spawnEntity(EntityTypes.NAUTILUS), true);
		assertFilterResult("Zombie Nautilus (neutral mob)", filter,
			() -> spawnEntity(EntityTypes.ZOMBIE_NAUTILUS), true);
		
		// Allowed because land-based
		assertFilterResult("Silverfish (land-based hostile mob)", filter,
			() -> spawnEntity(EntityTypes.SILVERFISH), true);
		assertFilterResult("Turtle (land-based mob)", filter,
			() -> spawnEntity(EntityTypes.TURTLE), true);
	}
}
