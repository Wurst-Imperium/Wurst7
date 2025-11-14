/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.world.level.block.Blocks;

public enum PistonTest
{
	;
	
	public static void testPistonDoesntCrash()
	{
		System.out.println(
			"Testing that a piston can extend and retract without crashing the game");
		
		// Place a redstone block and piston
		runChatCommand("setblock ~ ~1 ~2 minecraft:piston[facing=up]");
		waitForBlock(0, 1, 2, Blocks.PISTON);
		runChatCommand("setblock ~ ~ ~2 minecraft:redstone_block");
		waitForBlock(0, 0, 2, Blocks.REDSTONE_BLOCK);
		takeScreenshot("piston_extending");
		waitForWorldTicks(3);
		
		// Destroy the redstone block
		runChatCommand("setblock ~ ~ ~2 minecraft:air");
		waitForBlock(0, 0, 2, Blocks.AIR);
		takeScreenshot("piston_retracting");
		waitForWorldTicks(3);
		
		// Clean up
		runChatCommand("setblock ~ ~1 ~2 minecraft:air");
		waitForBlock(0, 1, 2, Blocks.AIR);
		clearChat();
	}
}
