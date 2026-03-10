/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Items;
import net.wurstclient.gametest.SingleplayerTest;

public final class CopyItemCmdTest extends SingleplayerTest
{
	public CopyItemCmdTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing .copyitem command");
		
		input.pressKey(GLFW.GLFW_KEY_F5);
		input.pressKey(GLFW.GLFW_KEY_F5);
		clearInventory();
		clearChat();
		context.waitTick();
		
		// Put on a golden helmet
		runCommand("item replace entity @s armor.head with golden_helmet");
		clearToasts();
		context.waitTicks(2);
		world.waitForChunksRender();
		context.takeScreenshot("copyitem_command_setup");
		assertNoItemInSlot(0);
		assertOneItemInSlot(39, Items.GOLDEN_HELMET);
		
		// .copyitem the helmet
		runWurstCommand("copyitem Wurst-Bot head");
		clearToasts();
		context.takeScreenshot("copyitem_command_result");
		assertOneItemInSlot(0, Items.GOLDEN_HELMET);
		assertOneItemInSlot(39, Items.GOLDEN_HELMET);
		
		// Clean up
		input.pressKey(GLFW.GLFW_KEY_F5);
		clearInventory();
		clearChat();
		waitForHandSwing();
	}
}
