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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.wurstclient.gametest.SingleplayerTest;

public final class ModifyCmdTest extends SingleplayerTest
{
	public ModifyCmdTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing .modify command");
		
		// /give a diamond
		runCommand("give @s diamond");
		context.waitTick();
		assertOneItemInSlot(0, Items.DIAMOND);
		
		// .modify it with NBT data
		runWurstCommand("modify set custom_name {\"text\":\"$cRed Name\"}");
		assertOneItemInSlot(0, Items.DIAMOND);
		ItemStack stack = context
			.computeOnClient(mc -> mc.player.getInventory().getSelectedItem());
		String name = stack.getComponents()
			.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty())
			.getString();
		if(!name.equals("\u00a7cRed Name"))
			throw new RuntimeException("Custom name is wrong: " + name);
		runWurstCommand("viewcomp type name");
		context.takeScreenshot("modify_command_result");
		
		// Clean up
		clearInventory();
		clearChat();
		waitForHandSwing();
	}
}
