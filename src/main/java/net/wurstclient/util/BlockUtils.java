/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.wurstclient.WurstClient;

public enum BlockUtils
{
	;
	
	private static final MinecraftClient MC = WurstClient.MC;
	
	public static BlockState getState(BlockPos pos)
	{
		return MC.world.getBlockState(pos);
	}
	
	public static Block getBlock(BlockPos pos)
	{
		return getState(pos).getBlock();
	}
	
	public static int getId(BlockPos pos)
	{
		return Block.getRawIdFromState(getState(pos));
	}
	
	public static String getName(BlockPos pos)
	{
		return getName(getBlock(pos));
	}
	
	public static String getName(Block block)
	{
		return Registry.BLOCK.getId(block).toString();
	}
	
	public static Block getBlockFromName(String name)
	{
		try
		{
			return Registry.BLOCK.get(new Identifier(name));
			
		}catch(InvalidIdentifierException e)
		{
			return Blocks.AIR;
		}
	}
	
	public static float getHardness(BlockPos pos)
	{
		return getState(pos).calcBlockBreakingDelta(MC.player, MC.world, pos);
	}
	
	private static VoxelShape getOutlineShape(BlockPos pos)
	{
		return getState(pos).getOutlineShape(MC.world, pos);
	}
	
	public static Box getBoundingBox(BlockPos pos)
	{
		return getOutlineShape(pos).getBoundingBox().offset(pos);
	}
	
	public static boolean canBeClicked(BlockPos pos)
	{
		return getOutlineShape(pos) != VoxelShapes.empty();
	}
	
	public static ArrayList<BlockPos> getAllInBox(BlockPos from, BlockPos to)
	{
		ArrayList<BlockPos> blocks = new ArrayList<>();
		
		BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()),
			Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
		BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()),
			Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
		
		for(int x = min.getX(); x <= max.getX(); x++)
			for(int y = min.getY(); y <= max.getY(); y++)
				for(int z = min.getZ(); z <= max.getZ(); z++)
					blocks.add(new BlockPos(x, y, z));
				
		return blocks;
	}
}
