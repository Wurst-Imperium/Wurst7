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

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.NativeImage;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotComparisonAlgorithm;
import net.fabricmc.fabric.impl.client.gametest.screenshot.TestScreenshotComparisonAlgorithms.RawImageImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.wurstclient.gametest.SingleplayerTest;

public final class AntiWobbleHackTest extends SingleplayerTest
{
	public AntiWobbleHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing AntiWobble hack");
		
		// Hide the hand, hack list and overlays
		input.pressKey(GLFW.GLFW_KEY_F1);
		Path reference = context.takeScreenshot("antiwobble_reference");
		
		for(String effect : new String[]{"nausea", "portal"})
		{
			for(String state : new String[]{"off", "on"})
			{
				runWurstCommand("t AntiWobble " + state);
				
				// Enable the effect at full strength with no fade-in
				context.runOnClient(mc -> {
					if(effect.equals("nausea"))
					{
						mc.player.addEffect(new MobEffectInstance(
							MobEffects.NAUSEA, 1200, 0, false, false));
						mc.player.getEffect(MobEffects.NAUSEA).skipBlending();
					}else
					{
						mc.player.portalEffectIntensity = 1;
						mc.player.oPortalEffectIntensity = 1;
					}
				});
				
				String name = "antiwobble_" + effect + "_" + state;
				Path screenshot = context.takeScreenshot(name);
				if(imagesMatch(reference, screenshot) != state.equals("on"))
					failWithScreenshot(name + "_failure",
						"AntiWobble test failed",
						"Expected " + effect + " wobble with AntiWobble "
							+ state + " to be "
							+ (state.equals("on") ? "hidden." : "visible."));
			}
			
			// Clear the current effect before testing the next one
			context.runOnClient(mc -> {
				mc.player.removeEffect(MobEffects.NAUSEA);
				mc.player.portalEffectIntensity = 0;
				mc.player.oPortalEffectIntensity = 0;
			});
		}
		
		runWurstCommand("t AntiWobble off");
		input.pressKey(GLFW.GLFW_KEY_F1);
	}
	
	private boolean imagesMatch(Path referencePath, Path screenshotPath)
	{
		try(NativeImage reference = loadImageFile(referencePath);
			NativeImage screenshot = loadImageFile(screenshotPath))
		{
			return TestScreenshotComparisonAlgorithm
				.meanSquaredDifference(3e-4F)
				.findColor(RawImageImpl.fromColorNativeImage(screenshot),
					RawImageImpl.fromColorNativeImage(reference)) != null;
		}
	}
}
