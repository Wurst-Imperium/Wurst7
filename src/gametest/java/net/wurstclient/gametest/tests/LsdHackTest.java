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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
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
		waitFor(LsdHackTest::isLsdRequested,
			"LSD post-effect did not activate.");
		
		// At this phase, the wool should look green.
		context.waitFor(mc -> mc.level.getGameTime() % 20 == 5);
		Path enabledPath = context.takeScreenshot("lsd_enabled");
		assertLsdChangedImage(disabledPath, enabledPath);
		
		// Change post-effects on the server
		Identifier invert = Identifier.withDefaultNamespace("invert");
		runCommand("posteffect add @s minecraft:invert");
		waitFor(mc -> mc.player.getActivePostEffects().contains(invert),
			"Server post-effect was not received.");
		assertLsdRequested("after the server added a post-effect");
		runCommand("posteffect clear @s");
		waitFor(mc -> mc.player.getActivePostEffects().isEmpty(),
			"Server post-effects were not cleared.");
		assertLsdRequested("after the server cleared post-effects");
		
		// LSD should survive a respawn
		LocalPlayer oldPlayer = context.computeOnClient(mc -> mc.player);
		runCommand("gamerule immediate_respawn true");
		runCommand("kill @s");
		waitFor(
			mc -> mc.player != null && mc.player != oldPlayer
				&& mc.player.isAlive() && mc.gui.screen() == null,
			"Player did not respawn.");
		assertLsdRequested("after respawning");
		
		// Clean up
		runCommand("gamerule immediate_respawn false");
		runCommand("tp @s 0 -57 0 0 0");
		waitForHandSwing();
		runWurstCommand("t LSD off");
		waitFor(mc -> !isLsdRequested(mc),
			"LSD post-effect did not deactivate.");
		setBlocksAndWait(blocks -> blocks.fill(-12, -60, 10, 12, -48, 10,
			Blocks.SMOOTH_STONE));
		clearParticles();
		clearChat();
	}
	
	private static boolean isLsdRequested(Minecraft mc)
	{
		return mc.gameRenderer.getRequestedPostEffects()
			.contains(LsdHack.LSD_POST_EFFECT);
	}
	
	private void assertLsdRequested(String situation)
	{
		context.waitTick();
		waitFor(LsdHackTest::isLsdRequested,
			"LSD post-effect was lost " + situation + ".");
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
