/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.gametest.WurstTest;

public enum PistonTest
{
	;
	
	public static void testPistonDoesntCrash(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info(
			"Testing that a piston can extend and retract without crashing the game");
		TestClientWorldContext world = spContext.getClientWorld();
		TestServerContext server = spContext.getServer();
		
		// Place a redstone block and piston
		runCommand(server, "setblock ~ ~1 ~2 minecraft:piston[facing=up]");
		waitForBlock(context, 0, 1, 2, Blocks.PISTON);
		runCommand(server, "setblock ~ ~ ~2 minecraft:redstone_block");
		waitForBlock(context, 0, 0, 2, Blocks.REDSTONE_BLOCK);
		context.waitTicks(3);
		world.waitForChunksRender();
		context.takeScreenshot("piston_extended");
		
		// Destroy the redstone block
		runCommand(server, "setblock ~ ~ ~2 minecraft:air");
		waitForBlock(context, 0, 0, 2, Blocks.AIR);
		context.waitTicks(3);
		world.waitForChunksRender();
		context.takeScreenshot("piston_retracted");
		
		// Clean up
		runCommand(server, "setblock ~ ~1 ~2 minecraft:air");
		waitForBlock(context, 0, 1, 2, Blocks.AIR);
		clearChat(context);
		context.waitTick();
		world.waitForChunksRender();
	}
}
