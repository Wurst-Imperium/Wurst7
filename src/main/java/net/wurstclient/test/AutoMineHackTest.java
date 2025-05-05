/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.block.Blocks;

public enum AutoMineHackTest
{
	;
	
	public static void testAutoMineHack()
	{
		System.out.println("Testing AutoMine hack");
		runChatCommand("gamemode survival");
		
		// Break a dirt block in survival mode
		runChatCommand("setblock ~ ~1 ~2 minecraft:dirt");
		waitForBlock(0, 1, 2, Blocks.DIRT);
		runWurstCommand("t AutoMine on");
		waitForBlock(0, 1, 2, Blocks.AIR);
		takeScreenshot("automine_survival");
		
		// Clean up
		runWurstCommand("t AutoMine off");
		runChatCommand("gamemode creative");
		runChatCommand("kill @e[type=item]");
		runChatCommand("clear");
		clearChat();
	}
}
