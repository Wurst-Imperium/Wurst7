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
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
		
		Identifier lsd = LsdHack.LSD_POST_EFFECT;
		Identifier invert = Identifier.withDefaultNamespace("invert");
		
		// Swap gray smooth stone background with red wool so that
		// the hue shift is easy to detect
		setBlocksAndWait(blocks -> blocks.fill(-12, -60, 10, 12, -48, 10,
			Blocks.WOOL.red()));
		context.waitTick();
		Path disabledPath = context.takeScreenshot("lsd_disabled");
		runWurstCommand("t LSD on");
		waitForAppliedEffects("after enabling LSD", lsd);
		
		// At this phase, the wool should look green
		context.waitFor(mc -> mc.level.getGameTime() % 20 == 5);
		Path enabledPath = context.takeScreenshot("lsd_enabled");
		assertLsdChangedImage(disabledPath, enabledPath);
		
		// Change post-effects on the server
		runCommand("posteffect add @s minecraft:invert");
		waitForAppliedEffects("after the server added invert", invert, lsd);
		runCommand("posteffect clear @s");
		waitForAppliedEffects("after the server cleared post-effects", lsd);
		
		// LSD should survive a respawn
		LocalPlayer oldPlayer = context.computeOnClient(mc -> mc.player);
		runCommand("gamerule immediate_respawn true");
		runCommand("kill @s");
		waitFor(
			mc -> mc.player != null && mc.player != oldPlayer
				&& mc.player.isAlive() && mc.gui.screen() == null,
			"Player did not respawn.");
		waitForAppliedEffects("after respawning", lsd);
		
		// Clean up
		runCommand("gamerule immediate_respawn false");
		runCommand("tp @s 0 -57 0 0 0");
		waitForHandSwing();
		runWurstCommand("t LSD off");
		waitForAppliedEffects("after disabling LSD");
		setBlocksAndWait(blocks -> blocks.fill(-12, -60, 10, 12, -48, 10,
			Blocks.SMOOTH_STONE));
		clearParticles();
		clearChat();
	}
	
	private void waitForAppliedEffects(String situation, Identifier... effects)
	{
		List<Identifier> expected = List.of(effects);
		waitFor(mc -> mc.gameRenderer.getAppliedPostEffects().equals(expected),
			"Expected post-effects " + expected + " " + situation + ".");
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
