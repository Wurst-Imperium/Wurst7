/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest.tests;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.wurstclient.WurstClient;
import net.wurstclient.gametest.SingleplayerTest;

public final class OcclusionCullingTest extends SingleplayerTest
{
	public OcclusionCullingTest(ClientGameTestContext context,
		TestSingleplayerContext spContext)
	{
		super(context, spContext);
	}
	
	@Override
	protected void runImpl()
	{
		// Prepare test rig
		logger.info("Testing occlusion culling in Freecam");
		teleportPlayer(200, -60, 200);
		setBlockAndWait(200, -56, 248, Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, Direction.NORTH));
		fillSurroundingSections(Blocks.SMOOTH_STONE);
		
		// Test that the chest is hidden without Freecam
		// Changing FOV makes the visibility graph refresh.
		context.runOnClient(mc -> mc.options.fov().set(71));
		assertChestVisibility(false,
			"Occlusion culling did not hide the test chest");
		
		// Test that the chest becomes visible with Freecam
		context.runOnClient(
			mc -> WurstClient.INSTANCE.getHax().freecamHack.setEnabled(true));
		context.runOnClient(mc -> mc.options.fov().set(70));
		assertChestVisibility(true,
			"Occlusion culling was not disabled in Freecam");
		
		// Clean up
		context.runOnClient(
			mc -> WurstClient.INSTANCE.getHax().freecamHack.setEnabled(false));
		setBlockAndWait(200, -56, 248, Blocks.AIR);
		fillSurroundingSections(Blocks.AIR);
		teleportPlayer(0, -57, 0);
	}
	
	private void teleportPlayer(int x, int y, int z)
	{
		runCommand("tp @s " + x + " " + y + " " + z + " 0 0");
		context.waitFor(
			mc -> mc.player.blockPosition().equals(new BlockPos(x, y, z)));
		world.waitForChunksRender();
	}
	
	private void fillSurroundingSections(Block block)
	{
		fillAndWait(192, -60, 208, 207, -49, 223, block);
		fillAndWait(192, -60, 176, 207, -49, 191, block);
		fillAndWait(208, -60, 192, 223, -49, 207, block);
		fillAndWait(176, -60, 192, 191, -49, 207, block);
		fillAndWait(192, -48, 192, 207, -33, 207, block);
	}
	
	private void assertChestVisibility(boolean expected, String errorMsg)
	{
		BlockPos chestPos = new BlockPos(200, -56, 248);
		waitFor(mc -> {
			Camera camera = mc.gameRenderer.getMainCamera();
			LevelRenderState state = new LevelRenderState();
			mc.getBlockEntityRenderDispatcher().prepare(camera);
			mc.levelRenderer.extractVisibleBlockEntities(camera, 1, state);
			boolean actual = state.blockEntityRenderStates.stream()
				.anyMatch(beState -> chestPos.equals(beState.blockPos));
			return actual == expected;
		}, 20, errorMsg);
	}
}
