/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientWorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.gametest.WurstTest;

public enum XRayHackTest
{
	;
	
	public static void testXRayHack(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		WurstTest.LOGGER.info("Testing X-Ray hack");
		TestInput input = context.getInput();
		TestClientWorldContext world = spContext.getClientWorld();
		TestServerContext server = spContext.getServer();
		buildTestRig(context, spContext);
		
		// Enable X-Ray with default settings
		runWurstCommand(context, "setcheckbox X-Ray only_show_exposed off");
		runWurstCommand(context, "setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading(context, world);
		assertScreenshotEquals(context, "xray_default",
			"https://i.imgur.com/Dftamqv.png");
		
		// Exposed only
		runWurstCommand(context, "setcheckbox X-Ray only_show_exposed on");
		runWurstCommand(context, "setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading(context, world);
		assertScreenshotEquals(context, "xray_exposed_only",
			"https://i.imgur.com/QlEpQTu.png");
		
		// Opacity mode
		runWurstCommand(context, "setcheckbox X-Ray only_show_exposed off");
		runWurstCommand(context, "setslider X-Ray opacity 0.5");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading(context, world);
		assertScreenshotEquals(context, "xray_opacity",
			"https://i.imgur.com/0nLulJn.png");
		
		// Exposed only + opacity
		runWurstCommand(context, "setcheckbox X-Ray only_show_exposed on");
		runWurstCommand(context, "setslider X-Ray opacity 0.5");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading(context, world);
		assertScreenshotEquals(context, "xray_exposed_only_opacity",
			"https://i.imgur.com/noPWDUl.png");
		
		// Clean up
		runCommand(server, "fill ~-5 ~-2 ~5 ~5 ~5 ~7 air");
		waitForBlock(context, 5, 5, 7, Blocks.AIR);
		runWurstCommand(context, "setcheckbox X-Ray only_show_exposed off");
		runWurstCommand(context, "setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading(context, world);
		clearChat(context);
	}
	
	private static void buildTestRig(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		TestServerContext server = spContext.getServer();
		TestClientWorldContext world = spContext.getClientWorld();
		
		// Stone wall (9 wide, 7 high, 3 deep)
		runCommand(server, "fill ~-5 ~-2 ~5 ~5 ~5 ~7 stone");
		
		// Ores (1 exposed and 1 hidden each)
		runCommand(server, "fill ~-4 ~1 ~5 ~-4 ~1 ~6 minecraft:coal_ore");
		runCommand(server, "fill ~-2 ~1 ~5 ~-2 ~1 ~6 minecraft:iron_ore");
		runCommand(server, "fill ~0 ~1 ~5 ~0 ~1 ~6 minecraft:gold_ore");
		runCommand(server, "fill ~2 ~1 ~5 ~2 ~1 ~6 minecraft:diamond_ore");
		runCommand(server, "fill ~4 ~1 ~5 ~4 ~1 ~6 minecraft:emerald_ore");
		runCommand(server, "fill ~-4 ~3 ~5 ~-4 ~3 ~6 minecraft:lapis_ore");
		runCommand(server, "fill ~-2 ~3 ~5 ~-2 ~3 ~6 minecraft:redstone_ore");
		runCommand(server, "fill ~0 ~3 ~5 ~0 ~3 ~6 minecraft:copper_ore");
		runCommand(server, "fill ~2 ~3 ~5 ~2 ~3 ~6 minecraft:nether_gold_ore");
		runCommand(server,
			"fill ~4 ~3 ~5 ~4 ~3 ~6 minecraft:nether_quartz_ore");
		
		// Fluids
		runCommand(server, "setblock ~1 ~0 ~6 minecraft:water");
		runCommand(server, "setblock ~-1 ~0 ~6 minecraft:lava");
		
		// Wait for blocks to appear
		waitForBlock(context, -1, 0, 6, Blocks.LAVA);
		waitForChunkReloading(context, world);
		clearChat(context);
	}
	
	private static void waitForChunkReloading(ClientGameTestContext context,
		TestClientWorldContext world)
	{
		// Wait longer if testing with Sodium, since we can't rely on
		// waitForChunksRender() to track when Sodium finishes loading chunks
		context.waitTicks(WurstTest.IS_MOD_COMPAT_TEST ? 5 : 1);
		world.waitForChunksRender();
	}
}
