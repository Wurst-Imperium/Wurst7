/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.client.option.Perspective;
import net.minecraft.item.Items;

public enum CopyItemCmdTest
{
	;
	
	public static void testCopyItemCmd()
	{
		System.out.println("Testing .copyitem command");
		
		// Put on a golden helmet
		runChatCommand("give @s golden_helmet");
		rightClickInGame();
		assertOneItemInSlot(39, Items.GOLDEN_HELMET);
		
		// .copyitem the helmet
		String playerName = submitAndGet(mc -> mc.player.getName().getString());
		runWurstCommand("copyitem " + playerName + " head");
		setPerspective(Perspective.THIRD_PERSON_FRONT);
		takeScreenshot("copyitem_command_result");
		assertOneItemInSlot(0, Items.GOLDEN_HELMET);
		
		// Clean up
		setPerspective(Perspective.FIRST_PERSON);
		runChatCommand("clear");
		clearChat();
	}
}
