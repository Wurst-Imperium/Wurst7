/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import net.wurstclient.gametest.tests.*;

public class WurstTest implements FabricClientGameTest
{
	public static final Logger LOGGER = LoggerFactory.getLogger("Wurst Test");
	
	public static final boolean IS_MOD_COMPAT_TEST =
		System.getProperty("wurst.test.withMods") != null;
	
	@Override
	public void runTest(ClientGameTestContext context)
	{
		LOGGER.info("Starting Wurst Client GameTest");
		hideSplashTexts(context);
		waitForTitleScreenFade(context);
		
		LOGGER.info("Reached title screen");
		assertScreenshotEquals(context, "title_screen",
			"https://i.imgur.com/4fSJRpd.png");
		
		AltManagerTest.testAltManagerButton(context);
		
		LOGGER.info("Creating test world");
		TestWorldBuilder worldBuilder = context.worldBuilder();
		worldBuilder.adjustSettings(creator -> {
			String mcVersion = SharedConstants.getGameVersion().name();
			creator.setWorldName("E2E Test " + mcVersion);
			creator.setGameMode(WorldCreator.Mode.CREATIVE);
			creator.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK)
				.set(false, null);
			applyFlatPresetWithSmoothStone(creator);
		});
		
		try(TestSingleplayerContext spContext = worldBuilder.create())
		{
			testInWorld(context, spContext);
			LOGGER.info("Exiting test world");
		}
		
		LOGGER.info("Test complete");
	}
	
	private void testInWorld(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		TestInput input = context.getInput();
		TestClientWorldContext world = spContext.getClientWorld();
		TestServerContext server = spContext.getServer();
		
		runCommand(server, "time set noon");
		runCommand(server, "tp 0 -57 0");
		runCommand(server, "fill ~ ~-3 ~ ~ ~-1 ~ smooth_stone");
		runCommand(server, "fill ~-12 ~-3 ~10 ~12 ~9 ~10 smooth_stone");
		
		LOGGER.info("Loading chunks");
		context.waitTicks(2);
		world.waitForChunksRender();
		
		assertScreenshotEquals(context, "in_game",
			IS_MOD_COMPAT_TEST ? "https://i.imgur.com/VxbGFrb.png"
				: "https://i.imgur.com/2UvurYl.png");
		
		LOGGER.info("Recording debug menu");
		input.pressKey(GLFW.GLFW_KEY_F3);
		context.takeScreenshot("debug_menu");
		input.pressKey(GLFW.GLFW_KEY_F3);
		
		LOGGER.info("Checking for broken mixins");
		MixinEnvironment.getCurrentEnvironment().audit();
		
		LOGGER.info("Opening inventory");
		input.pressKey(GLFW.GLFW_KEY_E);
		assertScreenshotEquals(context, "inventory",
			"https://i.imgur.com/LyQ5FSD.png");
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		
		LOGGER.info("Opening game menu");
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		assertScreenshotEquals(context, "game_menu",
			"https://i.imgur.com/L58HCGj.png");
		input.pressKey(GLFW.GLFW_KEY_ESCAPE);
		
		runWurstCommand(context,
			"setmode WurstLogo visibility only_when_outdated");
		runWurstCommand(context, "setcheckbox HackList animations off");
		
		// TODO: Open ClickGUI and Navigator
		
		// Test Wurst hacks
		AutoMineHackTest.testAutoMineHack(context, spContext);
		FreecamHackTest.testFreecamHack(context, spContext);
		NoFallHackTest.testNoFallHack(context, spContext);
		XRayHackTest.testXRayHack(context, spContext);
		
		// Test Wurst commands
		CopyItemCmdTest.testCopyItemCmd(context, spContext);
		GiveCmdTest.testGiveCmd(context, spContext);
		ModifyCmdTest.testModifyCmd(context, spContext);
		
		// TODO: Test more Wurst features
		
		// Test special cases
		PistonTest.testPistonDoesntCrash(context, spContext);
		
		// TODO: Check Wurst Options
	}
	
	// because the grass texture is randomized and smooth stone isn't
	private void applyFlatPresetWithSmoothStone(WorldCreator creator)
	{
		FlatChunkGeneratorConfig config =
			((FlatChunkGenerator)creator.getGeneratorOptionsHolder()
				.selectedDimensions().getChunkGenerator()).getConfig();
		
		List<FlatChunkGeneratorLayer> layers =
			List.of(new FlatChunkGeneratorLayer(1, Blocks.BEDROCK),
				new FlatChunkGeneratorLayer(2, Blocks.DIRT),
				new FlatChunkGeneratorLayer(1, Blocks.SMOOTH_STONE));
		
		creator.applyModifier((drm, dorHolder) -> dorHolder.with(drm,
			new FlatChunkGenerator(config.with(layers,
				config.getStructureOverrides(), config.getBiome()))));
	}
}
