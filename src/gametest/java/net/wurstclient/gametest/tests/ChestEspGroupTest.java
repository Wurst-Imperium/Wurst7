/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import java.util.List;
import java.util.Objects;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.gametest.WurstTest;
import net.wurstclient.hacks.chestesp.ChestEspBlockGroup;
import net.wurstclient.hacks.chestesp.ChestEspEntityGroup;
import net.wurstclient.hacks.chestesp.ChestEspGroupManager;

public final class ChestEspGroupTest extends SingleplayerTest
{
	public ChestEspGroupTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing ChestESP group matching");
		ChestEspGroupManager gm = new ChestEspGroupManager();
		
		assertMatchesOnly(gm, Blocks.CHEST, gm.normalChests);
		for(Block copperChest : List.of(Blocks.COPPER_CHEST,
			Blocks.EXPOSED_COPPER_CHEST, Blocks.WEATHERED_COPPER_CHEST,
			Blocks.OXIDIZED_COPPER_CHEST, Blocks.WAXED_COPPER_CHEST,
			Blocks.WAXED_EXPOSED_COPPER_CHEST,
			Blocks.WAXED_WEATHERED_COPPER_CHEST,
			Blocks.WAXED_OXIDIZED_COPPER_CHEST))
			assertMatchesOnly(gm, copperChest, gm.normalChests);
		assertMatchesOnly(gm, Blocks.TRAPPED_CHEST, gm.trapChests);
		assertMatchesOnly(gm, Blocks.ENDER_CHEST, gm.enderChests);
		assertMatchesOnly(gm, Blocks.BARREL, gm.barrels);
		assertMatchesOnly(gm, Blocks.DECORATED_POT, gm.pots);
		assertMatchesOnly(gm, Blocks.SHULKER_BOX, gm.shulkerBoxes);
		assertMatchesOnly(gm, Blocks.HOPPER, gm.hoppers);
		assertMatchesOnly(gm, Blocks.DROPPER, gm.droppers);
		assertMatchesOnly(gm, Blocks.DISPENSER, gm.dispensers);
		assertMatchesOnly(gm, Blocks.CRAFTER, gm.crafters);
		assertMatchesOnly(gm, Blocks.FURNACE, gm.furnaces);
		
		assertMatchesOnly(gm, EntityType.CHEST_MINECART, gm.chestCarts);
		assertMatchesOnly(gm, EntityType.OAK_CHEST_BOAT, gm.chestBoats);
		assertMatchesOnly(gm, EntityType.BAMBOO_CHEST_RAFT, gm.chestBoats);
		assertMatchesOnly(gm, EntityType.HOPPER_MINECART, gm.hopperCarts);
		
		if(!WurstTest.IS_LOOTR_INSTALLED)
			return;
		
		assertMatchesOnly(gm, getLootrBlock("lootr_chest"), gm.normalChests);
		assertMatchesOnly(gm, getLootrBlock("lootr_inventory"),
			gm.normalChests);
		assertMatchesOnly(gm, getLootrBlock("lootr_trapped_chest"),
			gm.trapChests);
		assertMatchesOnly(gm, getLootrBlock("lootr_barrel"), gm.barrels);
		assertMatchesOnly(gm, getLootrBlock("lootr_shulker"), gm.shulkerBoxes);
	}
	
	private void assertMatchesOnly(ChestEspGroupManager gm, Block block,
		ChestEspBlockGroup expectedGroup)
	{
		BlockEntity blockEntity = Objects.requireNonNull(
			((EntityBlock)block).newBlockEntity(BlockPos.ZERO,
				block.defaultBlockState()),
			"Missing block entity for " + block);
		
		if(!expectedGroup.matches(blockEntity))
			throw new AssertionError(blockEntity.getClass().getName()
				+ " did not match expected group "
				+ expectedGroup.getClass().getSimpleName());
		
		for(ChestEspBlockGroup group : gm.blockGroups)
			if(group != expectedGroup && group.matches(blockEntity))
				throw new AssertionError(blockEntity.getClass().getName()
					+ " unexpectedly matched group "
					+ group.getClass().getSimpleName());
	}
	
	private void assertMatchesOnly(ChestEspGroupManager gm,
		EntityType<?> entityType, ChestEspEntityGroup expectedGroup)
	{
		Entity entity = Objects.requireNonNull(
			context.computeOnClient(
				mc -> entityType.create(mc.level, EntitySpawnReason.COMMAND)),
			"Could not create entity " + entityType);
		
		if(!expectedGroup.matches(entity))
			throw new AssertionError(
				entity.getClass().getName() + " did not match expected group "
					+ expectedGroup.getClass().getSimpleName());
		
		for(ChestEspEntityGroup group : gm.entityGroups)
			if(group != expectedGroup && group.matches(entity))
				throw new AssertionError(
					entity.getClass().getName() + " unexpectedly matched group "
						+ group.getClass().getSimpleName());
	}
	
	private Block getLootrBlock(String path)
	{
		Identifier id = Identifier.fromNamespaceAndPath("lootr", path);
		return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(
			() -> new IllegalStateException("Missing block " + id));
	}
}
