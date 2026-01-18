/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
		
		// Enable Freecam with default settings
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals(context, "freecam_start_inside",
			"https://i.imgur.com/jdSno3u.png");
		clearChat(context);
		
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
		clearChat(context);
		
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
		clearChat(context);
		
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
		clearChat(context);
		
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
		clearChat(context);
		
		// Revert to inside, then fly back and up a bit
		runWurstCommand(context, "setmode Freecam initial_position inside");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		input.holdKeyFor(GLFW.GLFW_KEY_S, 2);
		input.holdKeyFor(GLFW.GLFW_KEY_SPACE, 1);
		assertScreenshotEquals(context, "freecam_moved",
			"https://i.imgur.com/HxrcHbh.png");
		clearChat(context);
		
		// Enable tracer
		runWurstCommand(context, "setcheckbox Freecam tracer on");
		context.waitTick();
		assertScreenshotEquals(context, "freecam_tracer",
			"https://i.imgur.com/z3pQumc.png");
		clearChat(context);
		
		// Disable tracer and un-hide hand
		runWurstCommand(context, "setcheckbox Freecam tracer off");
		runWurstCommand(context, "setcheckbox Freecam hide_hand off");
		context.waitTick();
		assertScreenshotEquals(context, "freecam_with_hand",
			"https://i.imgur.com/6tahHsE.png");
		clearChat(context);
		
		// Clean up
		runWurstCommand(context, "setcheckbox Freecam hide_hand on");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		clearChat(context);
	}
}
