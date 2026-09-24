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
import net.minecraft.server.level.ServerPlayer;
import net.wurstclient.gametest.SingleplayerTest;

public final class DamageCmdTest extends SingleplayerTest
{
	public DamageCmdTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing .damage command");
		
		runCommand("gamemode survival");
		context.waitFor(mc -> !mc.player.getAbilities().instabuild);
		ServerPlayer player = server
			.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst());
		float healthBefore = player.getHealth();
		
		runWurstCommand("damage 7");
		waitFor(mc -> player.getHealth() < healthBefore,
			".damage no longer causes fall damage");
		
		// Clean up
		player.setHealth(player.getMaxHealth());
		runCommand("gamemode creative");
		context.waitFor(mc -> mc.player.getAbilities().instabuild
			&& mc.player.onGround() && mc.player.hurtTime == 0);
		clearChat();
	}
}
