/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public enum ModifyCmdTest
{
	;
	
	public static void testModifyCmd()
	{
		System.out.println("Testing .modify command");
		
		// /give a diamond
		runChatCommand("give @s diamond");
		assertOneItemInSlot(0, Items.DIAMOND);
		
		// .modify it with NBT data
		runWurstCommand("modify set custom_name {\"text\":\"$cRed Name\"}");
		assertOneItemInSlot(0, Items.DIAMOND);
		submitAndWait(mc -> {
			ItemStack stack = mc.player.getInventory().getSelectedStack();
			String name = stack.getComponents()
				.getOrDefault(DataComponentTypes.CUSTOM_NAME, Text.empty())
				.getString();
			if(!name.equals("\u00a7cRed Name"))
				throw new RuntimeException("Custom name is wrong: " + name);
		});
		runWurstCommand("viewcomp type name");
		takeScreenshot("modify_command_result");
		
		// Clean up
		runChatCommand("clear");
		clearChat();
	}
}
