/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Items;
import net.wurstclient.gametest.SingleplayerTest;

public final class GiveCmdTest extends SingleplayerTest
{
	public GiveCmdTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing .give command");
		
		runWurstCommand("give diamond");
		clearToasts();
		context.waitTick();
		assertOneItemInSlot(0, Items.DIAMOND);
		context.takeScreenshot("give_command_result");
		
		// Clean up
		clearInventory();
		clearChat();
		waitForHandSwing();
	}
}
