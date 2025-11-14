/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.WurstClientTestHelper.*;

import java.time.Duration;

import org.spongepowered.asm.mixin.MixinEnvironment;

import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public final class WurstE2ETestClient implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		if(System.getProperty("wurst.e2eTest") == null)
			return;
		
		Thread.ofVirtual().name("Wurst End-to-End Test")
			.uncaughtExceptionHandler((t, e) -> {
				e.printStackTrace();
				System.exit(1);
			}).start(this::runTests);
	}
	
	private void runTests()
	{
		System.out.println("Starting Wurst End-to-End Tests");
		waitForResourceLoading();
		
		if(submitAndGet(mc -> mc.options.onboardAccessibility))
		{
			System.out.println("Onboarding is enabled. Waiting for it");
			waitForScreen(AccessibilityOnboardingScreen.class);
			System.out.println("Reached onboarding screen");
			clickButton("gui.continue");
		}
		
		waitForScreen(TitleScreen.class);
		waitForTitleScreenFade();
		System.out.println("Reached title screen");
		takeScreenshot("title_screen", Duration.ZERO);
		
		submitAndWait(AltManagerTest::testAltManagerButton);
		// TODO: Test more of AltManager
		
		System.out.println("Clicking singleplayer button");
		clickButton("menu.singleplayer");
		
		if(submitAndGet(
			mc -> !mc.getLevelSource().findLevelCandidates().isEmpty()))
		{
			System.out.println("World list is not empty. Waiting for it");
			waitForScreen(SelectWorldScreen.class);
			System.out.println("Reached select world screen");
			takeScreenshot("select_world_screen");
			clickButton("selectWorld.create");
		}
		
		waitForScreen(CreateWorldScreen.class);
		System.out.println("Reached create world screen");
		
		// Set MC version as world name
		setTextFieldText(0,
			"E2E Test " + SharedConstants.getCurrentVersion().getName());
		// Select creative mode
		clickButton("selectWorld.gameMode");
		clickButton("selectWorld.gameMode");
		takeScreenshot("create_world_screen");
		
		System.out.println("Creating test world");
		clickButton("selectWorld.create");
		
		waitForWorldLoad();
		dismissTutorialToasts();
		waitForWorldTicks(200);
		runChatCommand("seed");
		System.out.println("Reached singleplayer world");
		takeScreenshot("in_game", Duration.ZERO);
		runChatCommand("gamerule doDaylightCycle false");
		runChatCommand("gamerule doWeatherCycle false");
		runChatCommand("gamerule doTraderSpawning false");
		runChatCommand("gamerule doPatrolSpawning false");
		runChatCommand("time set noon");
		clearChat();
		
		System.out.println("Opening debug menu");
		toggleDebugHud();
		takeScreenshot("debug_menu");
		
		System.out.println("Closing debug menu");
		toggleDebugHud();
		
		System.out.println("Checking for broken mixins");
		MixinEnvironment.getCurrentEnvironment().audit();
		
		System.out.println("Opening inventory");
		openInventory();
		takeScreenshot("inventory");
		
		System.out.println("Closing inventory");
		closeScreen();
		
		// TODO: Open ClickGUI and Navigator
		
		// Build a test platform and clear out the space above it
		runChatCommand("fill ~-7 ~-5 ~-7 ~7 ~-1 ~7 stone");
		runChatCommand("fill ~-7 ~ ~-7 ~7 ~30 ~7 air");
		runChatCommand("kill @e[type=!player,distance=..10]");
		
		// Clear inventory and chat before running tests
		runChatCommand("clear");
		clearChat();
		
		// Test Wurst hacks
		AutoMineHackTest.testAutoMineHack();
		FreecamHackTest.testFreecamHack();
		NoFallHackTest.testNoFallHack();
		XRayHackTest.testXRayHack();
		
		// Test Wurst commands
		CopyItemCmdTest.testCopyItemCmd();
		GiveCmdTest.testGiveCmd();
		ModifyCmdTest.testModifyCmd();
		
		// TODO: Test more Wurst features
		
		// Test special cases
		PistonTest.testPistonDoesntCrash();
		
		System.out.println("Opening game menu");
		openGameMenu();
		takeScreenshot("game_menu");
		
		// TODO: Check Wurst Options
		
		System.out.println("Returning to title screen");
		clickButton("menu.returnToMenu");
		waitForScreen(TitleScreen.class);
		
		System.out.println("Stopping the game");
		clickButton("menu.quit");
	}
}
