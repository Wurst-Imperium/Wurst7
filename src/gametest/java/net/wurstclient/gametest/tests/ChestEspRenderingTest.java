/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.wurstclient.WurstClient;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.hacks.ChestEspHack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.Setting;

public final class ChestEspRenderingTest extends SingleplayerTest
{
	public ChestEspRenderingTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing ChestESP rendering");
		ChestEspHack chestEsp = WurstClient.INSTANCE.getHax().chestEspHack;
		context.runOnClient(mc -> chestEsp.setEnabled(true));
		List<Entity> vehicles = buildTestRig();
		
		waitForScreenshotMatch("chestesp_default_settings",
			"https://i.imgur.com/BmAZ812.png");
		
		context.runOnClient(mc -> chestEsp.getSettings().values().stream()
			.filter(CheckboxSetting.class::isInstance)
			.map(CheckboxSetting.class::cast)
			.forEach(checkbox -> checkbox.setChecked(true)));
		assertScreenshotEquals("chestesp_boxes",
			"https://i.imgur.com/h0W56OS.png");
		
		runWurstCommand("setmode ChestESP style lines_only");
		assertScreenshotEquals("chestesp_lines",
			"https://i.imgur.com/VXqh0Ns.png");
		
		runWurstCommand("setmode ChestESP style lines_and_boxes");
		assertScreenshotEquals("chestesp_lines_and_boxes",
			"https://i.imgur.com/xq2Qes0.png");
		
		context.runOnClient(mc -> {
			List<ColorSetting> colorSettings = chestEsp.getSettings().values()
				.stream().filter(ColorSetting.class::isInstance)
				.map(ColorSetting.class::cast).toList();
			for(int i = 0; i < colorSettings.size(); i++)
			{
				Color color = new Color(
					Color.HSBtoRGB((float)i / colorSettings.size(), 0.8F, 1));
				colorSettings.get(i).setColor(color);
			}
		});
		assertScreenshotEquals("chestesp_custom_colors",
			"https://i.imgur.com/UR6gktJ.png");
		
		// Clean up
		context.runOnClient(mc -> {
			chestEsp.setEnabled(false);
			for(Setting setting : chestEsp.getSettings().values())
				setting.resetToDefault();
		});
		removeVehicles(vehicles);
		setBlocksAndWait(
			blocks -> blocks.fill(-5, -59, 6, 5, -53, 7, Blocks.AIR));
		context.waitTick();// to trigger ChestEspHack.onUpdate()
	}
	
	private List<Entity> buildTestRig()
	{
		BlockState topSlab = Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
			.setValue(SlabBlock.TYPE, SlabType.TOP);
		setBlocksAndWait(blocks -> {
			// Top row: normal chests
			blocks.set(5, -53, 7, Blocks.CHEST);
			blocks.set(3, -53, 7, chestState(Blocks.CHEST, ChestType.RIGHT));
			blocks.set(2, -53, 7, chestState(Blocks.CHEST, ChestType.LEFT));
			blocks.set(0, -53, 7, Blocks.ENDER_CHEST);
			blocks.set(-2, -53, 7, Blocks.TRAPPED_CHEST);
			blocks.set(-4, -53, 7,
				chestState(Blocks.TRAPPED_CHEST, ChestType.RIGHT));
			blocks.set(-5, -53, 7,
				chestState(Blocks.TRAPPED_CHEST, ChestType.LEFT));
			blocks.fill(5, -53, 6, -5, -53, 6, topSlab);
			
			// Second row: other containers
			blocks.set(5, -55, 7, Blocks.BARREL);
			blocks.set(3, -55, 7, Blocks.SHULKER_BOX);
			blocks.set(1, -55, 7, Blocks.DECORATED_POT);
			blocks.set(-1, -55, 7, Blocks.FURNACE);
			blocks.set(-3, -55, 7, Blocks.BLAST_FURNACE);
			blocks.set(-5, -55, 7, Blocks.SMOKER);
			blocks.fill(5, -55, 6, -5, -55, 6, topSlab);
			
			// Third row: redstone things
			blocks.set(5, -57, 7, Blocks.DISPENSER);
			blocks.set(3, -57, 7, Blocks.DROPPER);
			blocks.set(1, -57, 7, Blocks.HOPPER);
			blocks.set(-1, -57, 7, Blocks.CRAFTER);
			blocks.set(-3, -57, 7, Blocks.WAXED_EXPOSED_COPPER_CHEST);
			blocks.fill(5, -57, 6, -5, -57, 6, Blocks.SMOOTH_STONE_SLAB);
			
			// Fourth row: vehicle background
			blocks.fill(5, -59, 6, -5, -59, 6, Blocks.SMOOTH_STONE_SLAB);
		});
		
		List<Entity> vehicles =
			List.of(spawnEntity(EntityType.CHEST_MINECART, 5, -59, 7, 90),
				spawnEntity(EntityType.HOPPER_MINECART, 3, -59, 7, 90),
				spawnEntity(EntityType.OAK_CHEST_BOAT, 1, -59, 7, 180),
				spawnEntity(EntityType.BAMBOO_CHEST_RAFT, -1, -59, 7, 180));
		context.waitFor(mc -> vehicles.stream()
			.allMatch(vehicle -> mc.level.getEntity(vehicle.getId()) != null));
		context.waitTick();// to trigger ChestEspHack.onUpdate()
		return vehicles;
	}
	
	private BlockState chestState(Block block, ChestType type)
	{
		return block.defaultBlockState().setValue(ChestBlock.TYPE, type);
	}
	
	private <T extends Entity> T spawnEntity(EntityType<T> type, int x, int y,
		int z, float yRot)
	{
		return server.computeOnServer(mc -> {
			T entity = Objects.requireNonNull(
				type.create(mc.overworld(), EntitySpawnReason.COMMAND));
			entity.snapTo(x + 0.5, y, z + 0.5, yRot, 0);
			entity.setNoGravity(true);
			mc.overworld().addFreshEntity(entity);
			return entity;
		});
	}
	
	private void removeVehicles(List<Entity> vehicles)
	{
		List<Integer> entityIds = vehicles.stream().map(Entity::getId).toList();
		server.runOnServer(mc -> vehicles.forEach(Entity::discard));
		context.waitFor(mc -> entityIds.stream()
			.allMatch(id -> mc.level.getEntity(id) == null));
	}
}
