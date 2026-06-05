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

public abstract class EntityFilterTest extends SingleplayerTest
{
	public EntityFilterTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	protected final <T extends Entity> T spawnEntity(EntityType<T> type)
	{
		return server.computeOnServer(s -> {
			T e = type.create(s.overworld(), EntitySpawnReason.COMMAND);
			e.setPos(0.5, -56, 5);
			s.overworld().addFreshEntity(e);
			return e;
		});
	}
	
	protected final void assertFilterResult(String description,
		Supplier<EntityFilter> filterSupplier, Supplier<Entity> spawner,
		boolean expectedResult)
	{
		EntityFilter filter = filterSupplier.get();
		Entity entity = spawner.get();
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
