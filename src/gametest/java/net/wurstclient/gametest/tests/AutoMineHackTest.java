/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import net.minecraft.block.Blocks;
import net.wurstclient.gametest.WurstTest;

public enum AutoMineHackTest
{
	;
	
	public static void testAutoMineHack(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing AutoMine hack");
		TestClientWorldContext world = spContext.getClientWorld();
		TestServerContext server = spContext.getServer();
		runCommand(server, "gamemode survival");
		
		// Break a dirt block in survival mode
		runCommand(server, "setblock ~ ~1 ~2 minecraft:dirt");
		waitForBlock(context, 0, 1, 2, Blocks.DIRT);
		runWurstCommand(context, "t AutoMine on");
		waitForBlock(context, 0, 1, 2, Blocks.AIR);
		context.waitTick();
		world.waitForChunksRender();
		context.takeScreenshot("automine_survival");
		
		// Clean up
		runWurstCommand(context, "t AutoMine off");
		runCommand(server, "gamemode creative");
		runCommand(server, "kill @e[type=item]");
		clearInventory(context);
		context.waitTick();
		clearParticles(context);
		clearChat(context);
		context.waitTick();
	}
}
