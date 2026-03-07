/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.gametest.WurstTest;

public final class FreecamHackTest extends SingleplayerTest
{
	public FreecamHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing Freecam hack");
		
		// Enable Freecam with default settings
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals("freecam_start_inside",
			"https://i.imgur.com/jdSno3u.png");
		
		// Scroll to change speed
		input.scroll(1);
		context.waitTick();
		assertScreenshotEquals("freecam_speed_scrolled",
			"https://i.imgur.com/DysLqZw.png");
		runWurstCommand("setslider Freecam horizontal_speed 1");
		if(context.computeOnClient(
			mc -> mc.player.getInventory().getSelectedSlot()) != 0)
			throw new RuntimeException(
				"Scrolling while using Freecam with \"Scroll to change speed\" enabled changed the selected slot.");
		
		// Scroll to change selected slot
		runWurstCommand("setcheckbox Freecam scroll_to_change_speed off");
		input.scroll(1);
		context.waitTick();
		assertScreenshotEquals("freecam_hotbar_scrolled",
			"https://i.imgur.com/edjDUxr.png");
		if(context.computeOnClient(
			mc -> mc.player.getInventory().getSelectedSlot()) != 8)
			throw new RuntimeException(
				"Scrolling while using Freecam with \"Scroll to change speed\" disabled didn't change the selected slot.");
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		runWurstCommand("setcheckbox Freecam scroll_to_change_speed on");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Enable Freecam with initial position in front
		runWurstCommand("setmode Freecam initial_position in_front");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals("freecam_start_in_front",
			"https://i.imgur.com/nrMP191.png");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Enable Freecam with initial position above
		runWurstCommand("setmode Freecam initial_position above");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals("freecam_start_above",
			"https://i.imgur.com/3LbAtRj.png");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Revert to inside, then fly back and up a bit
		runWurstCommand("setmode Freecam initial_position inside");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		input.holdKeyFor(GLFW.GLFW_KEY_S, 2);
		input.holdKeyFor(GLFW.GLFW_KEY_SPACE, 1);
		assertScreenshotEquals("freecam_moved",
			"https://i.imgur.com/HxrcHbh.png");
		
		// Enable tracer
		runWurstCommand("setcheckbox Freecam tracer on");
		context.waitTick();
		assertScreenshotEquals("freecam_tracer",
			"https://i.imgur.com/z3pQumc.png");
		
		// Disable tracer and un-hide hand
		runWurstCommand("setcheckbox Freecam tracer off");
		runWurstCommand("setcheckbox Freecam hide_hand off");
		context.waitTick();
		assertScreenshotEquals("freecam_with_hand",
			"https://i.imgur.com/6tahHsE.png");
		runWurstCommand("setcheckbox Freecam hide_hand on");
		
		// Enable player movement, walk forward, and turn around
		runCommand("fill 0 -58 1 0 -58 2 smooth_stone");
		runWurstCommand("setmode Freecam apply_input_to player");
		input.holdKeyFor(GLFW.GLFW_KEY_W, 10);
		for(int i = 0; i < 10; i++)
		{
			input.moveCursor(120, 0);
			context.waitTick();
		}
		context.waitTick();
		assertScreenshotEquals("freecam_player_moved",
			"https://i.imgur.com/mf6NgQl.png");
		runWurstCommand("setmode Freecam apply_input_to camera");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Reset player and remove walkway
		runCommand("fill 0 -58 1 0 -58 2 air");
		runCommand("tp @s 0 -57 0 0 0");
		// Restore body rotation - /tp only rotates the head as of 1.21.11
		context.runOnClient(mc -> mc.player.setYBodyRot(0));
		
		// Test "Interact from" setting
		runCommand("setblock 0 -56 2 smooth_stone");
		waitForBlock(0, 1, 2, Blocks.SMOOTH_STONE);
		runCommand("setblock 0 -56 1 lever[face=wall,facing=north]");
		runCommand("setblock 0 -56 3 lever[face=wall,facing=south]");
		waitForBlock(0, 1, 3, Blocks.LEVER);
		context.waitTicks(WurstTest.IS_MOD_COMPAT_TEST ? 5 : 1);
		world.waitForChunksRender();
		context.takeScreenshot("freecam_interact_setup");
		
		// Enable Freecam and fly to a side view
		runWurstCommand("setslider Freecam horizontal_speed 0.95");
		input.pressKey(GLFW.GLFW_KEY_U);
		input.holdKeyFor(GLFW.GLFW_KEY_W, 3);
		context.waitTick();
		runWurstCommand("setslider Freecam horizontal_speed 1");
		for(int i = 0; i < 6; i++)
		{
			input.moveCursor(120, 0);
			context.waitTick();
		}
		input.holdKeyFor(GLFW.GLFW_KEY_S, 2);
		context.waitTick();
		world.waitForChunksRender();
		context.takeScreenshot("freecam_interact_side_view");
		
		// Right click with "Interact from: Player"
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTick();
		assertLeverState(0, -56, 1, true, "near lever, player mode");
		assertLeverState(0, -56, 3, false, "far lever, player mode");
		
		// Right click with "Interact from: Camera"
		runWurstCommand("setmode Freecam interact_from camera");
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTick();
		assertLeverState(0, -56, 3, true, "far lever, camera mode");
		assertLeverState(0, -56, 1, true, "near lever, camera mode");
		
		// Replace levers with chickens
		runCommand("fill 0 -56 1 0 -56 3 air strict");
		Chicken nearChicken = spawnChicken(1.5);
		Chicken farChicken = spawnChicken(3.5);
		clearParticles();
		context.waitTick();
		
		// Left click with "Interact from: Player"
		runWurstCommand("setmode Freecam interact_from player");
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTick();
		assertChickenHealth(nearChicken, true, "near chicken, player mode");
		assertChickenHealth(farChicken, false, "far chicken, player mode");
		
		// Left click with "Interact from: Camera"
		nearChicken.discard();
		nearChicken = spawnChicken(1.5);
		context.waitTick();
		runWurstCommand("setmode Freecam interact_from camera");
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTick();
		assertChickenHealth(farChicken, true, "far chicken, camera mode");
		assertChickenHealth(nearChicken, false, "near chicken, camera mode");
		
		// Clean up
		nearChicken.discard();
		farChicken.discard();
		runWurstCommand("setmode Freecam interact_from player");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTicks(2);
	}
	
	private Chicken spawnChicken(double z)
	{
		return server.computeOnServer(s -> {
			Chicken c = EntityType.CHICKEN.create(s.overworld(),
				EntitySpawnReason.COMMAND);
			c.setPos(0.5, -56, z);
			c.setNoAi(true);
			c.setNoGravity(true);
			s.overworld().addFreshEntity(c);
			return c;
		});
	}
	
	private void assertLeverState(int x, int y, int z, boolean expectedPowered,
		String description)
	{
		BlockState state = server.computeOnServer(
			s -> s.overworld().getBlockState(new BlockPos(x, y, z)));
		
		String errorMessage = null;
		if(state.getBlock() != Blocks.LEVER)
			errorMessage = "Expected lever at " + x + ", " + y + ", " + z + " ("
				+ description + ") but found " + state;
		else if(state.getValue(LeverBlock.POWERED) != expectedPowered)
			errorMessage = "Lever at " + x + ", " + y + ", " + z + " ("
				+ description + ") expected powered=" + expectedPowered
				+ " but was powered=" + !expectedPowered;
		
		if(errorMessage == null)
			return;
		
		failWithScreenshot("freecam_block_interaction_failed",
			"Freecam block interaction test failed", errorMessage);
	}
	
	private void assertChickenHealth(Chicken chicken, boolean expectedDamaged,
		String description)
	{
		float health = chicken.getHealth();
		boolean isDamaged = health < 4.0f;
		if(isDamaged == expectedDamaged)
			return;
		
		String errorMessage = "Chicken (" + description + ") expected "
			+ (expectedDamaged ? "damaged" : "full health") + " but had health="
			+ health;
		
		failWithScreenshot("freecam_entity_interaction_failed",
			"Freecam entity interaction test failed", errorMessage);
	}
}
