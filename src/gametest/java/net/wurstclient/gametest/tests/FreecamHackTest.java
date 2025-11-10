/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
		runWurstCommand(context, "setcheckbox Freecam tracer off");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		assertScreenshotEquals(context, "freecam_start",
			"https://i.imgur.com/RxkPywy.png");
		clearChat(context);
		
		// Fly back and up a bit
		input.holdKeyFor(GLFW.GLFW_KEY_S, 2);
		input.holdKeyFor(GLFW.GLFW_KEY_SPACE, 1);
		assertScreenshotEquals(context, "freecam_moved",
			"https://i.imgur.com/pZDlYfH.png");
		clearChat(context);
		
		// Enable tracer
		runWurstCommand(context, "setcheckbox Freecam tracer on");
		context.waitTick();
		assertScreenshotEquals(context, "freecam_tracer",
			"https://i.imgur.com/jYqDFzE.png");
		clearChat(context);
		
		// Clean up
		runWurstCommand(context, "setcheckbox Freecam tracer off");
		input.pressKey(GLFW.GLFW_KEY_U);
		context.waitTick();
		world.waitForChunksRender();
		clearChat(context);
	}
}
