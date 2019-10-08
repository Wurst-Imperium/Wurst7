/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
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
	
	public static boolean canBeClicked(BlockPos pos)
	{
		return getState(pos).getOutlineShape(MC.world, pos) != VoxelShapes
			.empty();
	}
	
	public static ArrayList<BlockPos> getAllInBox(BlockPos min, BlockPos max)
	{
		ArrayList<BlockPos> blocks = new ArrayList<>();
		
		for(int x = min.getX(); x <= max.getX(); x++)
			for(int y = min.getY(); y <= max.getY(); y++)
				for(int z = min.getZ(); z <= max.getZ(); z++)
					blocks.add(new BlockPos(x, y, z));
				
		return blocks;
	}
	
	public static Box getBoundingBox(BlockPos pos)
	{
		try
		{
			return getState(pos)
				.getCollisionShape(MinecraftClient.getInstance().world, pos)
				.offset(pos.getX(), pos.getY(), pos.getZ()).getBoundingBox();
		}catch(UnsupportedOperationException e)
		{
			return new Box(new BlockPos(0, 0, 0)); // Hackish solution to fix no
													// bounds for empty shape
													// crash (1.14.4 version
													// only tested). - Mersid.
		}
		
	}
}
