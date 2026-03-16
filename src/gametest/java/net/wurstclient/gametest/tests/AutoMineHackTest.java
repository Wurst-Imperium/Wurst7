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
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.gametest.SingleplayerTest;

public final class AutoMineHackTest extends SingleplayerTest
{
	public AutoMineHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing AutoMine hack");
		runCommand("gamemode survival");
		
		// Break a dirt block in survival mode
		runCommand("setblock ~ ~1 ~2 minecraft:dirt");
		waitForBlock(0, 1, 2, Blocks.DIRT);
		runWurstCommand("t AutoMine on");
		waitForBlock(0, 1, 2, Blocks.AIR);
		context.waitTick();
		world.waitForChunksRender();
		context.takeScreenshot("automine_survival");
		
		// Clean up
		runWurstCommand("t AutoMine off");
		runCommand("gamemode creative");
		runCommand("kill @e[type=item]");
		clearInventory();
		context.waitTick();
		clearParticles();
		clearChat();
		context.waitTick();
	}
}
