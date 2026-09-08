/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests.filters;

import static net.minecraft.world.entity.EntityTypes.*;
import static net.wurstclient.util.MobDisposition.*;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.util.MobDisposition;

public final class MobDispositionTest extends SingleplayerTest
{
	private final Set<EntityType<?>> testedMobs = new HashSet<>();
	
	public MobDispositionTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing mob classification");
		// Catch hierarchy changes and prompt a review when Minecraft adds mobs.
		checkTypes(PASSIVE, ALLAY, ARMADILLO, AXOLOTL, BAT, CAMEL, CAMEL_HUSK,
			CAT, CHICKEN, COD, COPPER_GOLEM, COW, DOLPHIN, DONKEY, FOX, FROG,
			HAPPY_GHAST, GLOW_SQUID, HORSE, LLAMA, MOOSHROOM, MULE, OCELOT,
			PANDA, PARROT, PIG, RABBIT, SALMON, SHEEP, SKELETON_HORSE, SLIME,
			SNIFFER, SNOW_GOLEM, SQUID, STRIDER, SULFUR_CUBE, TADPOLE,
			TRADER_LLAMA, TROPICAL_FISH, TURTLE, VILLAGER, WANDERING_TRADER,
			ZOMBIE_HORSE);
		checkTypes(NEUTRAL, BEE, ENDERMAN, GOAT, IRON_GOLEM, NAUTILUS, PIGLIN,
			POLAR_BEAR, PUFFERFISH, WOLF, ZOMBIFIED_PIGLIN, ZOMBIE_NAUTILUS);
		checkTypes(HOSTILE, BLAZE, BOGGED, BREEZE, CAVE_SPIDER, CREAKING,
			CREEPER, DROWNED, ELDER_GUARDIAN, ENDERMITE, ENDER_DRAGON, EVOKER,
			GHAST, GIANT, GUARDIAN, HOGLIN, HUSK, ILLUSIONER, MAGMA_CUBE,
			PARCHED, PHANTOM, PIGLIN_BRUTE, PILLAGER, RAVAGER, SHULKER,
			SILVERFISH, SKELETON, SPIDER, STRAY, VEX, VINDICATOR, WARDEN, WITCH,
			WITHER, WITHER_SKELETON, ZOGLIN, ZOMBIE, ZOMBIE_VILLAGER);
		
		server.runOnServer(s -> {
			for(EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
			{
				if(testedMobs.contains(type) || !BuiltInRegistries.ENTITY_TYPE
					.getKey(type).getNamespace().equals("minecraft"))
					continue;
				if(type.create(s.overworld(),
					EntitySpawnReason.COMMAND) instanceof Mob)
					throw new AssertionError(
						"Review new mob: " + type.toShortString());
			}
			
			var golem =
				IRON_GOLEM.create(s.overworld(), EntitySpawnReason.COMMAND);
			golem.setPlayerCreated(true);
			assertGroup(PASSIVE, golem);
			
			var panda = PANDA.create(s.overworld(), EntitySpawnReason.COMMAND);
			panda.setMainGene(Panda.Gene.AGGRESSIVE);
			assertGroup(NEUTRAL, panda);
			
			var rabbit =
				RABBIT.create(s.overworld(), EntitySpawnReason.COMMAND);
			rabbit.setComponent(DataComponents.RABBIT_VARIANT,
				Rabbit.Variant.EVIL);
			assertGroup(HOSTILE, rabbit);
			
			var piglin =
				PIGLIN.create(s.overworld(), EntitySpawnReason.COMMAND);
			piglin.setBaby(true);
			assertGroup(PASSIVE, piglin);
			
			var slime = SLIME.create(s.overworld(), EntitySpawnReason.COMMAND);
			slime.setSize(2, true);
			assertGroup(HOSTILE, slime);
		});
	}
	
	@SafeVarargs
	private void checkTypes(MobDisposition expected,
		EntityType<? extends Mob>... types)
	{
		server.runOnServer(s -> {
			for(EntityType<? extends Mob> type : types)
			{
				testedMobs.add(type);
				assertGroup(expected,
					type.create(s.overworld(), EntitySpawnReason.COMMAND));
			}
		});
	}
	
	private void assertGroup(MobDisposition expected, Mob mob)
	{
		MobDisposition actual = MobDisposition.of(mob);
		if(actual != expected)
			throw new AssertionError(
				"Expected " + expected + " for " + mob + ", got " + actual);
	}
}
