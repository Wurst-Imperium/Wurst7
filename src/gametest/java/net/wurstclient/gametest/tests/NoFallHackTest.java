/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.wurstclient.gametest.SingleplayerTest;

public final class NoFallHackTest extends SingleplayerTest
{
	public NoFallHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing NoFall hack");
		
		input.pressKey(GLFW.GLFW_KEY_F5);
		runCommand("gamemode survival");
		if(!context.computeOnClient(mc -> mc.player.onGround()))
			throw new RuntimeException("Player is not on ground");
		assertPlayerHealth(health -> health == 20);
		
		// Fall 5 blocks with NoFall enabled
		logger.info("Falling 5 blocks with NoFall enabled");
		runWurstCommand("t NoFall on");
		fall5Blocks();
		context.takeScreenshot("nofall_on_5_blocks");
		assertPlayerHealth(health -> health == 20);
		
		// Fall 5 blocks with NoFall disabled
		logger.info("Falling 5 blocks with NoFall disabled");
		runWurstCommand("t NoFall off");
		fall5Blocks();
		context.takeScreenshot("nofall_off_5_blocks");
		assertPlayerHealth(health -> health < 20);
		
		// Clean up
		context.runOnClient(mc -> mc.player.heal(999));
		runCommand("gamemode creative");
		input.pressKey(GLFW.GLFW_KEY_F5);
		input.pressKey(GLFW.GLFW_KEY_F5);
		clearChat();
		context.waitTicks(5);
	}
	
	private void fall5Blocks()
	{
		runCommand("tp ~ ~5 ~");
		context.waitFor(mc -> !mc.player.onGround());
		context.waitFor(mc -> mc.player.onGround());
		context.waitTick();
	}
	
	private void assertPlayerHealth(Predicate<Integer> healthCheck)
	{
		int health = context.computeOnClient(mc -> (int)mc.player.getHealth());
		if(!healthCheck.test(health))
			throw new RuntimeException("Player's health is wrong: " + health);
		
		logger.info("Player's health is correct: " + health);
	}
}
