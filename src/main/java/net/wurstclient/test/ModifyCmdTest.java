/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

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
		runWurstCommand("modify add {test:123}");
		assertOneItemInSlot(0, Items.DIAMOND);
		submitAndWait(mc -> {
			ItemStack stack = mc.player.getInventory().getMainHandStack();
			NbtCompound nbt = stack.getNbt().copy();
			if(!nbt.contains("test"))
				throw new RuntimeException(
					"NBT data is missing the 'test' key");
			if(nbt.getInt("test") != 123)
				throw new RuntimeException("NBT data is incorrect");
		});
		runWurstCommand("viewnbt");
		takeScreenshot("modify_command_result");
		
		// Clean up
		runChatCommand("clear");
		clearChat();
	}
}
