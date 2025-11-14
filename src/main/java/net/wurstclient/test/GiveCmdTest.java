/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.world.item.Items;

public enum GiveCmdTest
{
	;
	
	public static void testGiveCmd()
	{
		System.out.println("Testing .give command");
		runWurstCommand("give diamond");
		waitForWorldTicks(1);
		assertOneItemInSlot(0, Items.DIAMOND);
		
		// Clean up
		runChatCommand("clear");
		clearChat();
	}
}
