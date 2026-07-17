/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.wurstclient.gametest.SingleplayerTest;

public final class PauseScreenTest extends SingleplayerTest
{
	public PauseScreenTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Opening game menu");
		input.pressKey(InputConstants.KEY_ESCAPE);
		assertScreenshotEquals("game_menu", "https://i.imgur.com/WxuKtG6.png");
		
		logger.info("Opening Wurst Options screen");
		for(int i = 0; i < 7; i++)
			input.pressKey(InputConstants.KEY_TAB);
		input.pressKey(InputConstants.KEY_RETURN);
		assertScreenshotEquals("wurst_options_screen",
			"https://i.imgur.com/JpzrcP1.png");
		// TODO: Test manager screens
		input.pressKey(InputConstants.KEY_ESCAPE);
		
		logger.info("Opening statistics screen");
		for(int i = 0; i < 2; i++)
			input.pressKey(InputConstants.KEY_TAB);
		input.pressKey(InputConstants.KEY_RETURN);
		context.waitFor(mc -> mc.gui.screen() instanceof StatsScreen statsScreen
			&& !statsScreen.isLoading);
		assertScreenshotEquals("statistics_screen",
			"https://i.imgur.com/CPMAfzO.png");
		// TODO: Test Disable Wurst button
		input.pressKey(InputConstants.KEY_ESCAPE);
		input.pressKey(InputConstants.KEY_ESCAPE);
		
		testAlternativeWurstOptionsLocation();
	}
	
	private void testAlternativeWurstOptionsLocation()
	{
		runWurstCommand("setmode WurstOptions location statistics");
		
		logger.info("Opening game menu without Wurst Options");
		input.pressKey(InputConstants.KEY_ESCAPE);
		assertScreenshotEquals("game_menu_alt",
			"https://i.imgur.com/RdY7QPA.png");
		
		logger.info("Opening statistics screen with Wurst Options");
		for(int i = 0; i < 3; i++)
			input.pressKey(InputConstants.KEY_TAB);
		input.pressKey(InputConstants.KEY_RETURN);
		context.waitFor(mc -> mc.gui.screen() instanceof StatsScreen statsScreen
			&& !statsScreen.isLoading);
		assertScreenshotEquals("statistics_screen_alt",
			"https://i.imgur.com/e8q4hJo.png");
		input.pressKey(InputConstants.KEY_ESCAPE);
		input.pressKey(InputConstants.KEY_ESCAPE);
		
		runWurstCommand("setmode WurstOptions location game_menu");
	}
}
