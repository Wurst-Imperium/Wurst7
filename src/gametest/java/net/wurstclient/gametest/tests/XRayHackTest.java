/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.gametest.WurstTest;

public final class XRayHackTest extends SingleplayerTest
{
	public XRayHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing X-Ray hack");
		buildTestRig();
		
		// Enable X-Ray with default settings
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading();
		assertScreenshotEquals("xray_default",
			"https://i.imgur.com/Dftamqv.png");
		
		// Exposed only
		runWurstCommand("setcheckbox X-Ray only_show_exposed on");
		runWurstCommand("setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading();
		assertScreenshotEquals("xray_exposed_only",
			"https://i.imgur.com/QlEpQTu.png");
		
		// Opacity mode
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0.5");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading();
		assertScreenshotEquals("xray_opacity",
			WurstTest.IS_MOD_COMPAT_TEST ? "https://i.imgur.com/hXdzoDB.png"
				: "https://i.imgur.com/oZqevTx.png");
		
		// Exposed only + opacity
		runWurstCommand("setcheckbox X-Ray only_show_exposed on");
		runWurstCommand("setslider X-Ray opacity 0.5");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading();
		assertScreenshotEquals("xray_exposed_only_opacity",
			WurstTest.IS_MOD_COMPAT_TEST ? "https://i.imgur.com/ZwIARSr.png"
				: "https://i.imgur.com/3DLxNuS.png");
		
		// Clean up
		runCommand("fill ~-5 ~-2 ~4 ~5 ~5 ~7 air strict");
		waitForBlock(5, 5, 7, Blocks.AIR);
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		waitForChunkReloading();
		clearChat();
	}
	
	private void buildTestRig()
	{
		// Stone wall (9 wide, 7 high, 3 deep)
		runCommand("fill ~-5 ~-2 ~5 ~5 ~5 ~7 stone");
		
		// Ores (1 exposed and 1 hidden each)
		runCommand("fill ~-4 ~1 ~5 ~-4 ~1 ~6 minecraft:coal_ore");
		runCommand("fill ~-2 ~1 ~5 ~-2 ~1 ~6 minecraft:iron_ore");
		runCommand("fill ~0 ~1 ~5 ~0 ~1 ~6 minecraft:gold_ore");
		runCommand("fill ~2 ~1 ~5 ~2 ~1 ~6 minecraft:diamond_ore");
		runCommand("fill ~4 ~1 ~5 ~4 ~1 ~6 minecraft:emerald_ore");
		runCommand("fill ~-4 ~3 ~5 ~-4 ~3 ~6 minecraft:lapis_ore");
		runCommand("fill ~-2 ~3 ~5 ~-2 ~3 ~6 minecraft:redstone_ore");
		runCommand("fill ~0 ~3 ~5 ~0 ~3 ~6 minecraft:copper_ore");
		runCommand("fill ~2 ~3 ~5 ~2 ~3 ~6 minecraft:nether_gold_ore");
		runCommand("fill ~4 ~3 ~5 ~4 ~3 ~6 minecraft:nether_quartz_ore");
		
		// Fluids
		runCommand("setblock ~1 ~0 ~6 minecraft:water");
		runCommand("setblock ~-1 ~0 ~6 minecraft:lava");
		
		// Snow
		runCommand("fill ~-5 ~-1 ~4 ~5 ~-1 ~4 minecraft:stone");
		runCommand("fill ~-5 ~0 ~4 ~5 ~0 ~4 minecraft:snow");
		
		// Wait for blocks to appear
		waitForBlock(-1, 0, 6, Blocks.LAVA);
		waitForChunkReloading();
		clearChat();
	}
	
	private void waitForChunkReloading()
	{
		// Wait longer if testing with Sodium, since we can't rely on
		// waitForChunksRender() to track when Sodium finishes loading chunks
		context.waitTicks(WurstTest.IS_MOD_COMPAT_TEST ? 5 : 1);
		world.waitForChunksRender();
	}
}
