/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientLevelContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
		
		String testName = getClass().getSimpleName();
		int retries =
			waitForScreenshotMatch(testName.toLowerCase() + "_cleanup",
				"https://i.imgur.com/XF1SILt.png");
		
		if(retries > 0)
			logger.warn(testName + " needed " + retries
				+ " retries to get a valid cleanup screenshot. First view ALL"
				+ " screenshots from " + testName + " to understand what"
				+ " happened, then optionally retest. If this keeps happening,"
				+ " your timings are probably wrong. Otherwise it's likely a"
				+ " fluke, especially if you didn't change any gametest code.");
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
	
	protected final void waitFor(Predicate<Minecraft> predicate,
		String errorMsg)
	{
		waitFor(predicate, ClientGameTestContext.DEFAULT_TIMEOUT, errorMsg);
	}
	
	protected final void waitFor(Predicate<Minecraft> predicate, int timeout,
		String errorMsg)
	{
		try
		{
			context.waitFor(predicate, timeout);
			
		}catch(AssertionError e)
		{
			WurstClientTestHelper.ghSummary(errorMsg);
			throw new AssertionError(errorMsg);
		}
	}
	
	/**
	 * Builds a batch of block placements, runs the whole batch in one server
	 * task, and waits for every final block state to reach the client before
	 * waiting for the affected chunks to render.
	 *
	 * <p>
	 * If a position is set more than once, only its last state is placed.
	 */
	protected final void setBlocksAndWait(Consumer<BlockBatch> batchBuilder)
	{
		BlockBatch batch = new BlockBatch();
		batchBuilder.accept(batch);
		
		server.runOnServer(mc -> batch.blocks
			.forEach((pos, state) -> mc.getLevel(Level.OVERWORLD).setBlock(pos,
				state, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS)));
		context.waitFor(
			mc -> batch.blocks.entrySet().stream().allMatch(entry -> mc.level
				.getBlockState(entry.getKey()) == entry.getValue()));
		world.waitForChunksRender();
	}
	
	protected static final class BlockBatch
	{
		private final LinkedHashMap<BlockPos, BlockState> blocks =
			new LinkedHashMap<>();
		
		public void set(BlockPos pos, BlockState state)
		{
			blocks.put(pos.immutable(), state);
		}
		
		public void set(BlockPos pos, Block block)
		{
			set(pos, block.defaultBlockState());
		}
		
		public void set(int x, int y, int z, BlockState state)
		{
			set(new BlockPos(x, y, z), state);
		}
		
		public void set(int x, int y, int z, Block block)
		{
			set(x, y, z, block.defaultBlockState());
		}
		
		public void fill(BlockPos pos1, BlockPos pos2, BlockState state)
		{
			BlockPos.betweenClosed(pos1, pos2).forEach(pos -> set(pos, state));
		}
		
		public void fill(BlockPos pos1, BlockPos pos2, Block block)
		{
			fill(pos1, pos2, block.defaultBlockState());
		}
		
		public void fill(int x1, int y1, int z1, int x2, int y2, int z2,
			BlockState state)
		{
			fill(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2), state);
		}
		
		public void fill(int x1, int y1, int z1, int x2, int y2, int z2,
			Block block)
		{
			fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
		}
	}
	
	protected final void waitForBlock(int x, int y, int z, Block block)
	{
		BlockPos pos = new BlockPos(x, y, z);
		context.waitFor(mc -> mc.level.getBlockState(pos).getBlock() == block);
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
		context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(true));
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
		context.runOnClient(mc -> mc.gui.toastManager().clear());
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
	
	protected final int waitForScreenshotMatch(String fileName,
		String templateUrl)
	{
		return WurstClientTestHelper.waitForScreenshotMatch(context, fileName,
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
