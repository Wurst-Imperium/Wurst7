/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
		setPerspective(Perspective.THIRD_PERSON_FRONT);
		runChatCommand("clear");
		clearChat();
		
		// Put on a golden helmet
		runChatCommand("item replace entity @s armor.head with golden_helmet");
		takeScreenshot("copyitem_command_setup");
		assertNoItemInSlot(0);
		assertOneItemInSlot(39, Items.GOLDEN_HELMET);
		
		// .copyitem the helmet
		String playerName = submitAndGet(mc -> mc.player.getName().getString());
		runWurstCommand("copyitem " + playerName + " head");
		takeScreenshot("copyitem_command_result");
		assertOneItemInSlot(0, Items.GOLDEN_HELMET);
		assertOneItemInSlot(39, Items.GOLDEN_HELMET);
		
		// Clean up
		setPerspective(Perspective.FIRST_PERSON);
		runChatCommand("clear");
		clearChat();
	}
}
