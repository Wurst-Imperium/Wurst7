/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.treebot;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RotationUtils;

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
	
	public static boolean hasLineOfSight(BlockPos pos)
	{
		return getLineOfSightSide(RotationUtils.getEyesPos(), pos) != null;
	}
	
	public static Direction getLineOfSightSide(Vec3d eyesPos, BlockPos pos)
	{
		Direction[] sides = Direction.values();
		
		Vec3d relCenter = BlockUtils.getBoundingBox(pos)
			.offset(-pos.getX(), -pos.getY(), -pos.getZ()).getCenter();
		Vec3d center = Vec3d.of(pos).add(relCenter);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getVector();
			Vec3d relHitVec = new Vec3d(relCenter.x * dirVec.getX(),
				relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		double[] distancesSq = new double[sides.length];
		boolean[] linesOfSight = new boolean[sides.length];
		
		double distanceSqToCenter = eyesPos.squaredDistanceTo(center);
		for(int i = 0; i < sides.length; i++)
		{
			distancesSq[i] = eyesPos.squaredDistanceTo(hitVecs[i]);
			
			// no need to raytrace the rear sides,
			// they can't possibly have line of sight
			if(distancesSq[i] >= distanceSqToCenter)
				continue;
			
			linesOfSight[i] = BlockUtils.hasLineOfSight(eyesPos, hitVecs[i]);
		}
		
		Direction side = null;
		for(int i = 0; i < sides.length; i++)
		{
			// require line of sight
			if(!linesOfSight[i])
				continue;
			
			// start with the first side that has LOS
			if(side == null)
			{
				side = sides[i];
				continue;
			}
			
			// then pick the closest side
			if(distancesSq[i] < distancesSq[side.ordinal()])
				side = sides[i];
		}
		
		// will be null if no LOS was found
		return side;
	}
}
