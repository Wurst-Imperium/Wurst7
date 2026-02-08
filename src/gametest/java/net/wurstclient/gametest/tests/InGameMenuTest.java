/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;
import static net.wurstclient.gametest.WurstTest.*;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.achievement.StatsScreen;

public enum InGameMenuTest
{
	;
	
	public static void testMenuScreens(ClientGameTestContext context)
	{
		TestInput input = context.getInput();
		
		LOGGER.info("Opening game menu");
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		assertScreenshotEquals(context, "game_menu",
			"https://i.imgur.com/ruPsaNz.png");
		
		LOGGER.info("Opening Wurst Options screen");
		for(int i = 0; i < 7; i++)
			input.pressKey(GLFW.GLFW_KEY_TAB);
		input.pressKey(GLFW.GLFW_KEY_ENTER);
		assertScreenshotEquals(context, "wurst_options_screen",
			"https://i.imgur.com/ZCt7eiE.png");
		// TODO: Test manager screens
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		
		LOGGER.info("Opening statistics screen");
		for(int i = 0; i < 2; i++)
			input.pressKey(GLFW.GLFW_KEY_TAB);
		input.pressKey(GLFW.GLFW_KEY_ENTER);
		context.waitFor(mc -> mc.screen instanceof StatsScreen statsScreen
			&& !statsScreen.isLoading);
		assertScreenshotEquals(context, "statistics_screen",
			"https://i.imgur.com/CPMAfzO.png");
		// TODO: Test Disable Wurst button
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		
		testAlternativeWurstOptionsLocation(context);
	}
	
	private static void testAlternativeWurstOptionsLocation(
		ClientGameTestContext context)
	{
		TestInput input = context.getInput();
		runWurstCommand(context, "setmode WurstOptions location statistics");
		
		LOGGER.info("Opening game menu without Wurst Options");
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		assertScreenshotEquals(context, "game_menu_alt",
			"https://i.imgur.com/5Yrnje0.png");
		
		LOGGER.info("Opening statistics screen with Wurst Options");
		for(int i = 0; i < 3; i++)
			input.pressKey(GLFW.GLFW_KEY_TAB);
		input.pressKey(GLFW.GLFW_KEY_ENTER);
		context.waitFor(mc -> mc.screen instanceof StatsScreen statsScreen
			&& !statsScreen.isLoading);
		assertScreenshotEquals(context, "statistics_screen_alt",
			"https://i.imgur.com/e8q4hJo.png");
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		
		runWurstCommand(context, "setmode WurstOptions location game_menu");
	}
}
