/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.wurstclient.gametest.WurstTest;

public enum FreecamHackTest
{
	;
	
	public static void testFreecamHack(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing Freecam hack");
		TestInput input = context.getInput();
		TestClientWorldContext world = spContext.getClientWorld();
		TestServerContext server = spContext.getServer();
		
		// Enable Freecam with default settings
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals(context, "freecam_start_inside",
			"https://i.imgur.com/jdSno3u.png");
		
		// Scroll to change speed
		input.scroll(1);
		context.waitTick();
		assertScreenshotEquals(context, "freecam_speed_scrolled",
			"https://i.imgur.com/DysLqZw.png");
		runWurstCommand(context, "setslider Freecam horizontal_speed 1");
		if(context.computeOnClient(
			mc -> mc.player.getInventory().getSelectedSlot()) != 0)
			throw new RuntimeException(
				"Scrolling while using Freecam with \"Scroll to change speed\" enabled changed the selected slot.");
		
		// Scroll to change selected slot
		runWurstCommand(context,
			"setcheckbox Freecam scroll_to_change_speed off");
		input.scroll(1);
		context.waitTick();
		assertScreenshotEquals(context, "freecam_hotbar_scrolled",
			"https://i.imgur.com/edjDUxr.png");
		if(context.computeOnClient(
			mc -> mc.player.getInventory().getSelectedSlot()) != 8)
			throw new RuntimeException(
				"Scrolling while using Freecam with \"Scroll to change speed\" disabled didn't change the selected slot.");
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		runWurstCommand(context,
			"setcheckbox Freecam scroll_to_change_speed on");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Enable Freecam with initial position in front
		runWurstCommand(context, "setmode Freecam initial_position in_front");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals(context, "freecam_start_in_front",
			"https://i.imgur.com/nrMP191.png");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Enable Freecam with initial position above
		runWurstCommand(context, "setmode Freecam initial_position above");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals(context, "freecam_start_above",
			"https://i.imgur.com/3LbAtRj.png");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Revert to inside, then fly back and up a bit
		runWurstCommand(context, "setmode Freecam initial_position inside");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		input.holdKeyFor(GLFW.GLFW_KEY_S, 2);
		input.holdKeyFor(GLFW.GLFW_KEY_SPACE, 1);
		assertScreenshotEquals(context, "freecam_moved",
			"https://i.imgur.com/HxrcHbh.png");
		
		// Enable tracer
		runWurstCommand(context, "setcheckbox Freecam tracer on");
		context.waitTick();
		assertScreenshotEquals(context, "freecam_tracer",
			"https://i.imgur.com/z3pQumc.png");
		
		// Disable tracer and un-hide hand
		runWurstCommand(context, "setcheckbox Freecam tracer off");
		runWurstCommand(context, "setcheckbox Freecam hide_hand off");
		context.waitTick();
		assertScreenshotEquals(context, "freecam_with_hand",
			"https://i.imgur.com/6tahHsE.png");
		runWurstCommand(context, "setcheckbox Freecam hide_hand on");
		
		// Enable player movement, walk forward, and turn around
		runCommand(server, "fill 0 -58 1 0 -58 2 smooth_stone");
		runWurstCommand(context, "setmode Freecam apply_input_to player");
		input.holdKeyFor(GLFW.GLFW_KEY_W, 10);
		for(int i = 0; i < 10; i++)
		{
			input.moveCursor(120, 0);
			context.waitTick();
		}
		context.waitTick();
		assertScreenshotEquals(context, "freecam_player_moved",
			"https://i.imgur.com/mf6NgQl.png");
		runWurstCommand(context, "setmode Freecam apply_input_to camera");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		
		// Reset player and remove walkway
		runCommand(server, "fill 0 -58 1 0 -58 2 air");
		runCommand(server, "tp @s 0 -57 0 0 0");
		// Restore body rotation - /tp only rotates the head as of 1.21.11
		context.runOnClient(mc -> mc.player.setYBodyRot(0));
		
		// Test "Interact from" setting
		runCommand(server, "setblock 0 -56 2 smooth_stone");
		waitForBlock(context, 0, 1, 2, Blocks.SMOOTH_STONE);
		runCommand(server, "setblock 0 -56 1 lever[face=wall,facing=north]");
		runCommand(server, "setblock 0 -56 3 lever[face=wall,facing=south]");
		waitForBlock(context, 0, 1, 3, Blocks.LEVER);
		context.waitTicks(WurstTest.IS_MOD_COMPAT_TEST ? 5 : 1);
		world.waitForChunksRender();
		context.takeScreenshot("freecam_interact_setup");
		
		// Enable Freecam and fly to a side view
		runWurstCommand(context, "setslider Freecam horizontal_speed 0.95");
		input.pressKey(GLFW.GLFW_KEY_U);
		input.holdKeyFor(GLFW.GLFW_KEY_W, 3);
		context.waitTick();
		runWurstCommand(context, "setslider Freecam horizontal_speed 1");
		for(int i = 0; i < 6; i++)
		{
			input.moveCursor(120, 0);
			context.waitTick();
		}
		input.holdKeyFor(GLFW.GLFW_KEY_S, 2);
		context.waitTick();
		world.waitForChunksRender();
		context.takeScreenshot("freecam_interact_side_view");
		
		// Right click with "Interact from: Player" (default)
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTick();
		assertLeverState(context, spContext, 0, -56, 1, true,
			"near lever, player mode");
		assertLeverState(context, spContext, 0, -56, 3, false,
			"far lever, player mode");
		
		// Switch to "Interact from: Camera" and right click
		runWurstCommand(context, "setmode Freecam interact_from camera");
		input.pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTick();
		assertLeverState(context, spContext, 0, -56, 3, true,
			"far lever, camera mode");
		assertLeverState(context, spContext, 0, -56, 1, true,
			"near lever, camera mode");
		
		// Clean up
		runCommand(server, "fill 0 -56 1 0 -56 3 air");
		runWurstCommand(context, "setmode Freecam interact_from player");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
	}
	
	private static void assertLeverState(ClientGameTestContext context,
		TestSingleplayerContext spContext, int x, int y, int z,
		boolean expectedPowered, String description)
	{
		TestServerContext server = spContext.getServer();
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
		
		TestClientWorldContext world = spContext.getClientWorld();
		context.waitTick();
		world.waitForChunksRender();
		
		String fileName = "freecam_interact_failed";
		Path screenshotPath = context.takeScreenshot(fileName);
		ghSummary("### Freecam interact test failed");
		ghSummary(errorMessage);
		String url = tryUploadToImgur(screenshotPath);
		if(url != null)
			ghSummary("![" + fileName + "](" + url + ")");
		else
			ghSummary("Couldn't upload " + fileName
				+ ".png to Imgur. Check the Test Screenshots.zip artifact.");
		throw new RuntimeException(errorMessage);
	}
}
