/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.wurstclient.gametest.WurstTest;

public enum NoFallHackTest
{
	;
	
	public static void testNoFallHack(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing NoFall hack");
		TestInput input = context.getInput();
		TestServerContext server = spContext.getServer();
		
		input.pressKey(GLFW.GLFW_KEY_F5);
		runCommand(server, "gamemode survival");
		if(!context.computeOnClient(mc -> mc.player.onGround()))
			throw new RuntimeException("Player is not on ground");
		assertPlayerHealth(context, health -> health == 20);
		
		// Fall 5 blocks with NoFall enabled
		WurstTest.LOGGER.info("Falling 5 blocks with NoFall enabled");
		runWurstCommand(context, "t NoFall on");
		fall5Blocks(context, server);
		context.takeScreenshot("nofall_on_5_blocks");
		assertPlayerHealth(context, health -> health == 20);
		
		// Fall 5 blocks with NoFall disabled
		WurstTest.LOGGER.info("Falling 5 blocks with NoFall disabled");
		runWurstCommand(context, "t NoFall off");
		fall5Blocks(context, server);
		context.takeScreenshot("nofall_off_5_blocks");
		assertPlayerHealth(context, health -> health < 20);
		
		// Clean up
		context.runOnClient(mc -> mc.player.heal(999));
		runCommand(server, "gamemode creative");
		input.pressKey(GLFW.GLFW_KEY_F5);
		input.pressKey(GLFW.GLFW_KEY_F5);
		clearChat(context);
	}
	
	private static void fall5Blocks(ClientGameTestContext context,
		TestServerContext server)
	{
		runCommand(server, "tp ~ ~5 ~");
		context.waitFor(mc -> !mc.player.onGround());
		context.waitFor(mc -> mc.player.onGround());
		context.waitTick();
	}
	
	private static void assertPlayerHealth(ClientGameTestContext context,
		Predicate<Integer> healthCheck)
	{
		int health = context.computeOnClient(mc -> (int)mc.player.getHealth());
		if(!healthCheck.test(health))
			throw new RuntimeException("Player's health is wrong: " + health);
		
		WurstTest.LOGGER.info("Player's health is correct: " + health);
	}
}
