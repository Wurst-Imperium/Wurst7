/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientLevelContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public abstract class SingleplayerTest
{
	protected final ClientGameTestContext context;
	protected final TestSingleplayerContext spContext;
	protected final TestInput input;
	protected final TestClientLevelContext world;
	protected final TestServerContext server;
	protected final Logger logger = WurstTest.LOGGER;
	
	public SingleplayerTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		this.context = context;
		this.spContext = spContext;
		this.input = context.getInput();
		this.world = spContext.getClientLevel();
		this.server = spContext.getServer();
	}
	
	/**
	 * Runs the test and verifies cleanup afterward.
	 */
	public final void run()
	{
		runImpl();
		assertScreenshotEquals(
			getClass().getSimpleName().toLowerCase() + "_cleanup",
			"https://i.imgur.com/XF1SILt.png");
	}
	
	/**
	 * Implement the actual test logic here. The test is responsible for
	 * cleaning up after itself (disabling hacks, removing blocks, clearing
	 * chat/inventory/particles, etc.).
	 */
	protected abstract void runImpl();
	
	protected final void runCommand(String command)
	{
		WurstClientTestHelper.runCommand(server, command);
	}
	
	protected final void runWurstCommand(String command)
	{
		WurstClientTestHelper.runWurstCommand(context, command);
	}
	
	protected final void waitForBlock(int relX, int relY, int relZ, Block block)
	{
		context.waitFor(mc -> mc.level
			.getBlockState(mc.player.blockPosition().offset(relX, relY, relZ))
			.getBlock() == block);
	}
	
	/**
	 * Waits for the hand swing and equip animations to finish. Call this
	 * after any action that changes/clears the currently held item, if a
	 * screenshot is taken soon after (<1 second).
	 */
	protected final void waitForHandSwing()
	{
		context.waitFor(mc -> {
			var renderer =
				mc.getEntityRenderDispatcher().getItemInHandRenderer();
			return !mc.player.swinging && renderer.mainHandHeight == 1
				&& renderer.oMainHandHeight == 1;
		}, 20);
	}
	
	protected final void clearChat()
	{
		context.runOnClient(mc -> mc.gui.getChat().clearMessages(true));
	}
	
	protected final void clearInventory()
	{
		input.pressKey(GLFW.GLFW_KEY_T);
		input.typeChars("/clear");
		input.pressKey(GLFW.GLFW_KEY_ENTER);
		context.waitTicks(2);
	}
	
	protected final void clearParticles()
	{
		context.runOnClient(mc -> mc.particleEngine.clearParticles());
	}
	
	protected final void clearToasts()
	{
		context.runOnClient(mc -> mc.getToastManager().clear());
	}
	
	protected final void assertOneItemInSlot(int slot, Item item)
	{
		ItemStack stack = context
			.computeOnClient(mc -> mc.player.getInventory().getItem(slot));
		if(!stack.is(item) || stack.getCount() != 1)
			throw new RuntimeException("Expected 1 "
				+ item.getName(item.getDefaultInstance()).getString()
				+ " at slot " + slot + ", found " + stack.getCount() + " "
				+ stack.getItemName().getString() + " instead");
	}
	
	protected final void assertScreenshotEquals(String fileName,
		String templateUrl)
	{
		WurstClientTestHelper.assertScreenshotEquals(context, fileName,
			templateUrl);
	}
	
	protected final void failWithScreenshot(String fileName, String title,
		String errorMessage)
	{
		Path screenshotPath = context.takeScreenshot(fileName);
		
		WurstClientTestHelper
			.ghSummary("### " + title + "\n" + errorMessage + "\n");
		String url = WurstClientTestHelper.tryUploadToImgur(screenshotPath);
		if(url != null)
			WurstClientTestHelper.ghSummary("![" + fileName + "](" + url + ")");
		else
			WurstClientTestHelper.ghSummary("Couldn't upload " + fileName
				+ ".png to Imgur. Check the Test Screenshots.zip artifact.");
		
		throw new RuntimeException(title + ": " + errorMessage);
	}
	
	protected final void assertNoItemInSlot(int slot)
	{
		ItemStack stack = context
			.computeOnClient(mc -> mc.player.getInventory().getItem(slot));
		if(!stack.isEmpty())
			throw new RuntimeException("Expected no item in slot " + slot
				+ ", found " + stack.getCount() + " "
				+ stack.getItemName().getString() + " instead");
	}
}
