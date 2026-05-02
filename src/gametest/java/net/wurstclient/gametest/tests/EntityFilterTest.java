/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.gametest.WurstClientTestHelper;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;
import net.wurstclient.settings.filters.FilterPassiveWaterSetting;

public final class EntityFilterTest extends SingleplayerTest
{
	public EntityFilterTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing entity filters");
		testPassiveWaterMobsFilter();
	}
	
	private void testPassiveWaterMobsFilter()
	{
		Supplier<EntityFilter> filter =
			() -> new FilterPassiveWaterSetting("", true);
		
		// Filtered out
		assertFilterResult("Axolotl", filter,
			() -> spawnEntity(EntityType.AXOLOTL), false);
		assertFilterResult("Cod", filter, () -> spawnEntity(EntityType.COD),
			false);
		assertFilterResult("Dolphin", filter,
			() -> spawnEntity(EntityType.DOLPHIN), false);
		assertFilterResult("Glow Squid", filter,
			() -> spawnEntity(EntityType.GLOW_SQUID), false);
		assertFilterResult("Salmon", filter,
			() -> spawnEntity(EntityType.SALMON), false);
		assertFilterResult("Squid", filter, () -> spawnEntity(EntityType.SQUID),
			false);
		assertFilterResult("Tadpole", filter,
			() -> spawnEntity(EntityType.TADPOLE), false);
		assertFilterResult("Tropical Fish", filter,
			() -> spawnEntity(EntityType.TROPICAL_FISH), false);
		
		// Allowed because hostile
		assertFilterResult("Drowned (hostile mob)", filter,
			() -> spawnEntity(EntityType.DROWNED), true);
		assertFilterResult("Elder Guardian (hostile mob)", filter,
			() -> spawnEntity(EntityType.ELDER_GUARDIAN), true);
		assertFilterResult("Guardian (hostile mob)", filter,
			() -> spawnEntity(EntityType.GUARDIAN), true);
		assertFilterResult("Pufferfish (hostile mob)", filter,
			() -> spawnEntity(EntityType.PUFFERFISH), true);
		
		// Allowed because neutral
		assertFilterResult("Nautilus (neutral mob)", filter,
			() -> spawnEntity(EntityType.NAUTILUS), true);
		assertFilterResult("Zombie Nautilus (neutral mob)", filter,
			() -> spawnEntity(EntityType.ZOMBIE_NAUTILUS), true);
		
		// Allowed because land-based
		assertFilterResult("Silverfish (land-based hostile mob)", filter,
			() -> spawnEntity(EntityType.SILVERFISH), true);
		assertFilterResult("Turtle (land-based mob)", filter,
			() -> spawnEntity(EntityType.TURTLE), true);
	}
	
	private <T extends Entity> T spawnEntity(EntityType<T> type)
	{
		return server.computeOnServer(s -> {
			T e = type.create(s.overworld(), EntitySpawnReason.COMMAND);
			e.setPos(0.5, -56, 5);
			s.overworld().addFreshEntity(e);
			return e;
		});
	}
	
	private void assertFilterResult(String description,
		Supplier<EntityFilter> filterSupplier, Supplier<Entity> spawner,
		boolean expectedResult)
	{
		Entity entity = spawner.get();
		EntityFilter filter = filterSupplier.get();
		
		boolean result = filter.test(entity);
		entity.discard();
		
		if(result == expectedResult)
			return;
		
		String title = "Entity filter test failed";
		String errorMessage = "Expected \"" + filter.getSetting().getName()
			+ "\" setting to " + (expectedResult ? "allow" : "filter out") + " "
			+ description + " but it "
			+ (result ? "allowed it" : "filtered it out") + " instead.";
		
		WurstClientTestHelper
			.ghSummary("### " + title + "\n" + errorMessage + "\n");
		throw new RuntimeException(title + ": " + errorMessage);
	}
}
