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
import net.minecraft.world.InteractionHand;
import net.wurstclient.gametest.SingleplayerTest;

public final class NoShieldOverlayHackTest extends SingleplayerTest
{
	public NoShieldOverlayHackTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		logger.info("Testing NoShieldOverlay hack");
		
		// Reference screenshot with no item or hand
		moveHandOutOfView();
		Path referencePath =
			context.takeScreenshot("noshieldoverlay_reference");
		
		// Vanilla shield, should be lowered
		testItem(referencePath, "normal shield", "shield", true);
		
		// Default 0.5 makes custom shield items disappear entirely because
		// their item model isn't as tall as a normal shield.
		runWurstCommand("setslider NoShieldOverlay blocking_offset 0.3");
		
		// Custom shield, should be lowered
		testItem(referencePath, "custom shield",
			"iron_sword[consumable={consume_seconds:999999,animation:'block'}]",
			true);
		
		// Infinite bread, triggers isUsingItem() but should NOT be lowered
		// The long consume_seconds also prevents the eating animation from
		// moving the item up and down and creating particles.
		testItem(referencePath, "bread",
			"bread[consumable={consume_seconds:999999}]", false);
		
		// Clean up
		runWurstCommand("setslider NoShieldOverlay blocking_offset 0.5");
		clearInventory();
		waitForHandSwing();
	}
	
	private void moveHandOutOfView()
	{
		context.runOnClient(mc -> mc.getEntityRenderDispatcher()
			.getItemInHandRenderer().itemUsed(InteractionHand.MAIN_HAND));
	}
	
	private void testItem(Path referencePath, String name, String giveArg,
		boolean shouldBeLowered)
	{
		String nameForFiles = name.replace(" ", "_");
		
		// Give item
		runCommand("item replace entity @s weapon.mainhand with " + giveArg);
		context.waitTicks(2);
		clearToasts();
		waitForHandSwing();
		
		// Hack off + idle
		Path offIdlePath = context
			.takeScreenshot("noshieldoverlay_" + nameForFiles + "_off_idle");
		runWurstCommand("t NoShieldOverlay on");
		
		// Hack on + idle
		Path onIdlePath = context
			.takeScreenshot("noshieldoverlay_" + nameForFiles + "_on_idle");
		assertItemMovement(name, "idle", shouldBeLowered, referencePath,
			offIdlePath, onIdlePath);
		runWurstCommand("t NoShieldOverlay off");
		
		// Hack off + blocking
		input.holdMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTick();
		waitForHandSwing();
		Path offBlockingPath = context.takeScreenshot(
			"noshieldoverlay_" + nameForFiles + "_off_blocking");
		input.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		
		// Hack on + blocking
		runWurstCommand("t NoShieldOverlay on");
		input.holdMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		context.waitTick();
		waitForHandSwing();
		Path onBlockingPath = context
			.takeScreenshot("noshieldoverlay_" + nameForFiles + "_on_blocking");
		assertItemMovement(name, "blocking", shouldBeLowered, referencePath,
			offBlockingPath, onBlockingPath);
		input.releaseMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		runWurstCommand("t NoShieldOverlay off");
	}
	
	private void assertItemMovement(String name, String scenario,
		boolean shouldBeLowered, Path referencePath, Path hackOffPath,
		Path hackOnPath)
	{
		try(NativeImage reference = loadImageFile(referencePath);
			NativeImage hackOff = loadImageFile(hackOffPath);
			NativeImage hackOn = loadImageFile(hackOnPath))
		{
			double itemYOff = getItemCenterY(reference, hackOff,
				scenario + " with hack off and " + name);
			double itemYOn = getItemCenterY(reference, hackOn,
				scenario + " with hack on and " + name);
			int yShift = (int)(itemYOn - itemYOff);
			
			logger.info("Item " + name + " moved down by " + yShift
				+ " pixels while " + scenario + ".");
			
			if(shouldBeLowered)
			{
				if(yShift >= 30)
					return;
				
				throw new RuntimeException("Expected " + name
					+ " to move down by at least 30 pixels while " + scenario
					+ ", but it only moved " + yShift + " pixels.");
			}else
			{
				if(yShift == 0)
					return;
				
				throw new RuntimeException(
					"Expected " + name + " to stay in place while " + scenario
						+ ", but it moved " + yShift + " pixels.");
			}
			
		}catch(RuntimeException e)
		{
			failWithScreenshot(
				"noshieldoverlay_" + name.replace(" ", "_") + "_" + scenario
					+ "_failure",
				"NoShieldOverlay test failed", e.getMessage());
		}
	}
	
	private double getItemCenterY(NativeImage reference, NativeImage withItem,
		String description)
	{
		int width = withItem.getWidth();
		int height = withItem.getHeight();
		
		long pixelCount = 0;
		long ySum = 0;
		
		for(int y = height / 3; y < height; y++)
			for(int x = width / 2; x < width; x++)
				if(getColorDifference(reference.getPixel(x, y),
					withItem.getPixel(x, y)) > 0)
				{
					pixelCount++;
					ySum += y;
				}
			
		logger.info(
			"Detected " + pixelCount + " item pixels for " + description + ".");
		if(pixelCount < 2000)
			throw new RuntimeException(
				"Could not detect enough item pixels for " + description
					+ " (found " + pixelCount + ").");
		
		return (double)ySum / pixelCount;
	}
	
	private int getColorDifference(int color1, int color2)
	{
		int red1 = color1 & 0xFF;
		int green1 = color1 >> 8 & 0xFF;
		int blue1 = color1 >> 16 & 0xFF;
		
		int red2 = color2 & 0xFF;
		int green2 = color2 >> 8 & 0xFF;
		int blue2 = color2 >> 16 & 0xFF;
		
		return Math.abs(red1 - red2) + Math.abs(green1 - green2)
			+ Math.abs(blue1 - blue2);
	}
}
