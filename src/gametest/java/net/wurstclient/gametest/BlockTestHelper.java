/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.gametest;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestClientLevelContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum BlockTestHelper
{
	;
	
	public static void waitForBlock(ClientGameTestContext context, int x, int y,
		int z, Block block)
	{
		BlockPos pos = new BlockPos(x, y, z);
		context.waitFor(mc -> mc.level.getBlockState(pos).getBlock() == block);
	}
	
	/**
	 * Builds a batch of block placements, runs the whole batch in one server
	 * task, and waits for every final block state to reach the client before
	 * waiting for the affected chunks to render.
	 *
	 * <p>
	 * If a position is set more than once, only its last state is placed.
	 */
	public static void setBlocksAndWait(ClientGameTestContext context,
		TestSingleplayerContext spContext, Consumer<BlockBatch> batchBuilder)
	{
		TestClientLevelContext world = spContext.getClientLevel();
		TestServerContext server = spContext.getServer();
		BlockBatch batch = new BlockBatch();
		batchBuilder.accept(batch);
		
		server.runOnServer(mc -> batch.blocks
			.forEach((pos, state) -> mc.getLevel(Level.OVERWORLD).setBlock(pos,
				state, batch.updateFlags)));
		context.waitFor(
			mc -> batch.blocks.entrySet().stream().allMatch(entry -> mc.level
				.getBlockState(entry.getKey()) == entry.getValue()));
		world.waitForChunksRender();
	}
	
	public static final class BlockBatch
	{
		private final LinkedHashMap<BlockPos, BlockState> blocks =
			new LinkedHashMap<>();
		private int updateFlags =
			Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS;
		
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
		
		public void setUpdateFlags(int updateFlags)
		{
			if((updateFlags & Block.UPDATE_CLIENTS) == 0)
				throw new IllegalArgumentException(
					"Update flags must include Block.UPDATE_CLIENTS");
			
			this.updateFlags = updateFlags;
		}
	}
}
