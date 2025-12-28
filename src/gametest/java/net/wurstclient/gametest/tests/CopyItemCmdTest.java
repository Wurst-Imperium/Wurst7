/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Items;
import net.wurstclient.gametest.WurstTest;

public enum CopyItemCmdTest
{
	;
	
	public static void testCopyItemCmd(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing .copyitem command");
		TestInput input = context.getInput();
		TestClientWorldContext world = spContext.getClientWorld();
		TestServerContext server = spContext.getServer();
		
		input.pressKey(GLFW.GLFW_KEY_F5);
		input.pressKey(GLFW.GLFW_KEY_F5);
		clearInventory(context);
		clearChat(context);
		
		// Put on a golden helmet
		runCommand(server,
			"item replace entity @s armor.head with golden_helmet");
		clearToasts(context);
		context.waitTicks(2);
		world.waitForChunksRender();
		context.takeScreenshot("copyitem_command_setup");
		assertNoItemInSlot(context, 0);
		assertOneItemInSlot(context, 39, Items.GOLDEN_HELMET);
		
		// .copyitem the helmet
		runWurstCommand(context, "copyitem Wurst-Bot head");
		clearToasts(context);
		context.takeScreenshot("copyitem_command_result");
		assertOneItemInSlot(context, 0, Items.GOLDEN_HELMET);
		assertOneItemInSlot(context, 39, Items.GOLDEN_HELMET);
		
		// Clean up
		input.pressKey(GLFW.GLFW_KEY_F5);
		clearInventory(context);
		clearChat(context);
		context.waitTicks(7);
	}
}
