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
		for(EntityType<? extends Mob> type : List.of(EntityTypes.AXOLOTL,
			EntityTypes.COD, EntityTypes.DOLPHIN, EntityTypes.GLOW_SQUID,
			EntityTypes.SALMON, EntityTypes.SQUID, EntityTypes.TADPOLE,
			EntityTypes.TROPICAL_FISH))
			assertFilteredOut(type.toShortString(), filter,
				() -> spawnEntity(type));
		
		// Allowed because hostile
		for(EntityType<? extends Mob> type : List.of(EntityTypes.DROWNED,
			EntityTypes.ELDER_GUARDIAN, EntityTypes.GUARDIAN,
			EntityTypes.PUFFERFISH))
			assertAllowed(type.toShortString() + " (hostile mob)", filter,
				() -> spawnEntity(type));
		
		// Allowed because neutral
		for(EntityType<? extends Mob> type : List.of(EntityTypes.NAUTILUS,
			EntityTypes.ZOMBIE_NAUTILUS))
			assertAllowed(type.toShortString() + " (neutral mob)", filter,
				() -> spawnEntity(type));
		
		// Allowed because land-based
		for(EntityType<? extends Mob> type : List.of(EntityTypes.SILVERFISH,
			EntityTypes.TURTLE))
			assertAllowed(type.toShortString() + " (land-based mob)", filter,
				() -> spawnEntity(type));
	}
}
