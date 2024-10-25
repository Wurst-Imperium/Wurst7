/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.test;

import static net.wurstclient.test.fabric.FabricClientTestHelper.*;

import java.time.Duration;

import org.spongepowered.asm.mixin.MixinEnvironment;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

// It would be cleaner to have this test in src/test/java, but remapping that
// into a separate testmod is a whole can of worms.
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
			}).start(this::runTest);
	}
	
	private void runTest()
	{
		System.out.println("Starting Wurst End-to-End Test");
		waitForLoadingComplete();
		
		if(submitAndWait(mc -> mc.options.onboardAccessibility))
		{
			System.out.println("Onboarding is enabled. Waiting for it");
			waitForScreen(AccessibilityOnboardingScreen.class);
			System.out.println("Reached onboarding screen");
			clickScreenButton("gui.continue");
		}
		
		waitForScreen(TitleScreen.class);
		waitForTitleScreenFade();
		System.out.println("Reached title screen");
		takeScreenshot("title_screen", Duration.ZERO);
		
		submitAndWait(WurstClientTestHelper::testAltManagerButton);
		// TODO: Test more of AltManager
		
		System.out.println("Clicking singleplayer button");
		clickScreenButton("menu.singleplayer");
		
		if(submitAndWait(mc -> !mc.getLevelStorage().getLevelList().isEmpty()))
		{
			System.out.println("World list is not empty. Waiting for it");
			waitForScreen(SelectWorldScreen.class);
			System.out.println("Reached select world screen");
			takeScreenshot("select_world_screen");
			clickScreenButton("selectWorld.create");
		}
		
		waitForScreen(CreateWorldScreen.class);
		System.out.println("Reached create world screen");
		
		// Select creative mode
		clickScreenButton("selectWorld.gameMode");
		clickScreenButton("selectWorld.gameMode");
		takeScreenshot("create_world_screen");
		
		System.out.println("Creating test world");
		clickScreenButton("selectWorld.create");
		
		waitForWorldTicks(200);
		System.out.println("Reached singleplayer world");
		takeScreenshot("in_game", Duration.ZERO);
		
		System.out.println("Opening debug menu");
		enableDebugHud();
		takeScreenshot("debug_menu");
		
		System.out.println("Closing debug menu");
		enableDebugHud();// bad name, it actually toggles
		
		System.out.println("Checking for broken mixins");
		MixinEnvironment.getCurrentEnvironment().audit();
		
		// TODO: Test some Wurst hacks
		
		System.out.println("Opening inventory");
		openInventory();
		takeScreenshot("inventory");
		
		System.out.println("Closing inventory");
		closeScreen();
		
		// TODO: Open ClickGUI and Navigator
		
		System.out.println("Opening game menu");
		openGameMenu();
		takeScreenshot("game_menu");
		
		// TODO: Check Wurst Options
		
		System.out.println("Returning to title screen");
		clickScreenButton("menu.returnToMenu");
		waitForScreen(TitleScreen.class);
		
		System.out.println("Stopping the game");
		clickScreenButton("menu.quit");
	}
}
