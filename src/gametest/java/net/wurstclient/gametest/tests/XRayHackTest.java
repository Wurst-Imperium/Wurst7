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
		world.waitForChunksRender();
		waitForScreenshotMatch("xray_default",
			"https://i.imgur.com/Dftamqv.png");
		
		// Exposed only
		runWurstCommand("setcheckbox X-Ray only_show_exposed on");
		runWurstCommand("setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		world.waitForChunksRender();
		waitForScreenshotMatch("xray_exposed_only",
			"https://i.imgur.com/QlEpQTu.png");
		
		// Opacity mode
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0.5");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		world.waitForChunksRender();
		waitForScreenshotMatch("xray_opacity",
			WurstTest.IS_SODIUM_INSTALLED ? "https://i.imgur.com/hXdzoDB.png"
				: "https://i.imgur.com/oZqevTx.png");
		
		// Exposed only + opacity
		runWurstCommand("setcheckbox X-Ray only_show_exposed on");
		runWurstCommand("setslider X-Ray opacity 0.5");
		input.pressKey(GLFW.GLFW_KEY_X);
		input.pressKey(GLFW.GLFW_KEY_X);
		world.waitForChunksRender();
		waitForScreenshotMatch("xray_exposed_only_opacity",
			WurstTest.IS_SODIUM_INSTALLED ? "https://i.imgur.com/ZwIARSr.png"
				: "https://i.imgur.com/3DLxNuS.png");
		
		// Clean up
		setBlocksAndWait(
			blocks -> blocks.fill(-5, -59, 4, 5, -52, 7, Blocks.AIR));
		runWurstCommand("setcheckbox X-Ray only_show_exposed off");
		runWurstCommand("setslider X-Ray opacity 0");
		input.pressKey(GLFW.GLFW_KEY_X);
		world.waitForChunksRender();
	}
	
	private void buildTestRig()
	{
		setBlocksAndWait(blocks -> {
			// Stone wall (11 wide, 8 high, 3 deep)
			blocks.fill(-5, -59, 5, 5, -52, 7, Blocks.STONE);
			
			// Ores (1 exposed and 1 hidden each)
			blocks.fill(-4, -56, 5, -4, -56, 6, Blocks.COAL_ORE);
			blocks.fill(-2, -56, 5, -2, -56, 6, Blocks.IRON_ORE);
			blocks.fill(0, -56, 5, 0, -56, 6, Blocks.GOLD_ORE);
			blocks.fill(2, -56, 5, 2, -56, 6, Blocks.DIAMOND_ORE);
			blocks.fill(4, -56, 5, 4, -56, 6, Blocks.EMERALD_ORE);
			blocks.fill(-4, -54, 5, -4, -54, 6, Blocks.LAPIS_ORE);
			blocks.fill(-2, -54, 5, -2, -54, 6, Blocks.REDSTONE_ORE);
			blocks.fill(0, -54, 5, 0, -54, 6, Blocks.COPPER_ORE);
			blocks.fill(2, -54, 5, 2, -54, 6, Blocks.NETHER_GOLD_ORE);
			blocks.fill(4, -54, 5, 4, -54, 6, Blocks.NETHER_QUARTZ_ORE);
			
			// Fluids
			blocks.set(1, -57, 6, Blocks.WATER);
			blocks.set(-1, -57, 6, Blocks.LAVA);
			
			// Snow
			blocks.fill(-5, -58, 4, 5, -58, 4, Blocks.STONE);
			blocks.fill(-5, -57, 4, 5, -57, 4, Blocks.SNOW);
		});
	}
}
