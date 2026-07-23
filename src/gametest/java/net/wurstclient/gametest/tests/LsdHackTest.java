/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import static net.wurstclient.gametest.WurstClientTestHelper.*;

import java.nio.file.Path;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.level.block.Blocks;
import net.wurstclient.gametest.SingleplayerTest;
import net.wurstclient.hacks.LsdHack;

public final class LsdHackTest extends SingleplayerTest
{
	public LsdHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing LSD hack");
		
		// Swap gray smooth stone background with red wool so that
		// the hue shift is easy to detect
		setBlocksAndWait(blocks -> blocks.fill(-12, -60, 10, 12, -48, 10,
			Blocks.WOOL.red()));
		context.waitTick();
		Path disabledPath = context.takeScreenshot("lsd_disabled");
		
		runWurstCommand("t LSD on");
		waitFor(
			mc -> LsdHack.LSD_POST_EFFECT
				.equals(mc.gameRenderer.currentPostEffect()),
			"LSD post-effect did not activate.");
		
		// At this phase, the wool should look green.
		context.waitFor(mc -> mc.level.getGameTime() % 20 == 5);
		Path enabledPath = context.takeScreenshot("lsd_enabled");
		assertLsdChangedImage(disabledPath, enabledPath);
		
		// Clean up
		runWurstCommand("t LSD off");
		waitFor(mc -> mc.gameRenderer.currentPostEffect() == null,
			"LSD post-effect did not deactivate.");
		setBlocksAndWait(blocks -> blocks.fill(-12, -60, 10, 12, -48, 10,
			Blocks.SMOOTH_STONE));
	}
	
	private void assertLsdChangedImage(Path disabledPath, Path enabledPath)
	{
		try(NativeImage disabled = loadImageFile(disabledPath);
			NativeImage enabled = loadImageFile(enabledPath))
		{
			long totalDifference = 0;
			
			for(int y = 0; y < disabled.getHeight(); y++)
				for(int x = 0; x < disabled.getWidth(); x++)
					totalDifference += getColorDifference(
						disabled.getPixel(x, y), enabled.getPixel(x, y));
				
			int pixelCount = disabled.getWidth() * disabled.getHeight();
			double averageDifference = (double)totalDifference / pixelCount;
			
			logger.info("Average difference: " + averageDifference);
			if(averageDifference >= 120)
				return;
			
			failWithScreenshot("lsd_failure", "LSD shader test failed",
				"Expected LSD shader to make the red wool look green, but the"
					+ " average color difference was only "
					+ averageDifference);
		}
	}
}
