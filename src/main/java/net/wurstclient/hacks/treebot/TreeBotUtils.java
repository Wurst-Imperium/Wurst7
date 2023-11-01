/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.treebot;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.util.BlockUtils;

public enum TreeBotUtils
{
	;
	
	private static final List<Block> LOG_BLOCKS =
		Arrays.asList(Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG,
			Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG);
	
	private static final List<Block> LEAVES_BLOCKS = Arrays.asList(
		Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
		Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES);
	
	public static boolean isLog(BlockPos pos)
	{
		return LOG_BLOCKS.contains(BlockUtils.getBlock(pos));
	}
	
	public static boolean isLeaves(BlockPos pos)
	{
		return LEAVES_BLOCKS.contains(BlockUtils.getBlock(pos));
	}
}
