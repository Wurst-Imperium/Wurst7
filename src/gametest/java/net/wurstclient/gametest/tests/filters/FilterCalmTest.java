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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.panda.Panda;
import net.wurstclient.gametest.tests.EntityFilterTest;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;
import net.wurstclient.settings.filters.AttackDetectingEntityFilter.Mode;
import net.wurstclient.settings.filters.FilterEndermenSetting;
import net.wurstclient.settings.filters.FilterNeutralSetting;
import net.wurstclient.settings.filters.FilterZombiePiglinsSetting;

public final class FilterCalmTest extends EntityFilterTest
{
	public FilterCalmTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing unusual anger signals");
		Supplier<EntityFilter> neutral =
			() -> FilterNeutralSetting.genericCombat(Mode.IF_CALM);
		
		for(EntityFilter filter : List.of(neutral.get(),
			FilterEndermenSetting.genericCombat(Mode.IF_CALM)))
		{
			assertFilteredOut("calm enderman", () -> filter,
				() -> spawnEntity(EntityTypes.ENDERMAN));
			assertAllowed("angry enderman", () -> filter, () -> {
				var enderman = spawnEntity(EntityTypes.ENDERMAN);
				enderman.setTarget(enderman.level().players().getFirst());
				return enderman;
			});
		}
		
		// Panda.isAggressive() describes its personality, not its attack flag.
		assertFilteredOut("calm aggressive-gene panda", neutral, () -> {
			var panda = spawnEntity(EntityTypes.PANDA);
			panda.setMainGene(Panda.Gene.AGGRESSIVE);
			return panda;
		});
		assertAllowed("attacking aggressive-gene panda", neutral, () -> {
			var panda = spawnEntity(EntityTypes.PANDA);
			panda.setMainGene(Panda.Gene.AGGRESSIVE);
			panda.setAggressive(true);
			return panda;
		});
		
		// Unlike most NeutralMobs, wolves and bees synchronize their anger.
		EntityFilter neutralFilter = neutral.get();
		for(EntityType<? extends Mob> type : List.of(EntityTypes.WOLF,
			EntityTypes.BEE))
		{
			Mob mob = spawnEntity(type);
			mob.setNoAi(true);
			context.waitFor(mc -> mc.level.getEntity(mob.getId()) != null);
			Entity clientMob =
				context.computeOnClient(mc -> mc.level.getEntity(mob.getId()));
			if(neutralFilter.test(clientMob))
				throw new AssertionError(
					"Calm " + type.toShortString() + " should be filtered out");
			((NeutralMob)mob).startPersistentAngerTimer();
			waitFor(mc -> neutralFilter.test(clientMob),
				"Angry " + type.toShortString() + " should pass the filter");
			((NeutralMob)mob).stopBeingAngry();
			waitFor(mc -> !neutralFilter.test(clientMob), "Calm "
				+ type.toShortString() + " should be filtered out again");
			mob.discard();
		}
		
		// Let vanilla add/remove the speed modifier, rather than injecting it.
		var piglin = spawnEntity(EntityTypes.ZOMBIFIED_PIGLIN);
		context.waitFor(mc -> mc.level.getEntity(piglin.getId()) != null);
		Entity clientPiglin =
			context.computeOnClient(mc -> mc.level.getEntity(piglin.getId()));
		EntityFilter zombiePiglinFilter =
			FilterZombiePiglinsSetting.genericCombat(Mode.IF_CALM);
		if(neutralFilter.test(clientPiglin)
			|| zombiePiglinFilter.test(clientPiglin))
			throw new AssertionError(
				"Calm zombie piglin should be filtered out");
		piglin.startPersistentAngerTimer();
		waitFor(
			mc -> neutralFilter.test(clientPiglin)
				&& zombiePiglinFilter.test(clientPiglin),
			"Angry zombie piglin should pass both filters");
		piglin.stopBeingAngry();
		waitFor(
			mc -> !neutralFilter.test(clientPiglin)
				&& !zombiePiglinFilter.test(clientPiglin),
			"Calm zombie piglin should be filtered out again");
		piglin.discard();
		context.waitFor(mc -> clientPiglin.isRemoved());
		clearParticles();
	}
}
