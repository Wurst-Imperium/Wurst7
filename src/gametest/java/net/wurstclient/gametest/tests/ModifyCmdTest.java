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
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.wurstclient.gametest.WurstTest;

public enum ModifyCmdTest
{
	;
	
	public static void testModifyCmd(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing .modify command");
		TestServerContext server = spContext.getServer();
		
		// /give a diamond
		runCommand(server, "give @s diamond");
		context.waitTick();
		assertOneItemInSlot(context, 0, Items.DIAMOND);
		
		// .modify it with NBT data
		runWurstCommand(context,
			"modify set custom_name {\"text\":\"$cRed Name\"}");
		assertOneItemInSlot(context, 0, Items.DIAMOND);
		ItemStack stack = context
			.computeOnClient(mc -> mc.player.getInventory().getSelectedStack());
		String name = stack.getComponents()
			.getOrDefault(DataComponentTypes.CUSTOM_NAME, Text.empty())
			.getString();
		if(!name.equals("\u00a7cRed Name"))
			throw new RuntimeException("Custom name is wrong: " + name);
		runWurstCommand(context, "viewcomp type name");
		context.takeScreenshot("modify_command_result");
		
		// Clean up
		clearInventory(context);
		clearChat(context);
		context.waitTicks(7);
	}
}
