/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.WurstClient;
import net.wurstclient.mixinterface.IMinecraftClient;
import net.wurstclient.util.BlockBreaker.BlockBreakingParams;

public enum BlockPlacer
{
	;
	
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final MinecraftClient MC = WurstClient.MC;
	private static final IMinecraftClient IMC = WurstClient.IMC;
	
	public static boolean placeOneBlock(BlockPos pos)
	{
		BlockPlacingParams params = getBlockPlacingParams(pos);
		if(params == null)
			return false;
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(params.hitVec);
		
		// place block
		IMC.getInteractionManager().rightClickBlock(params.neighbor,
			params.side, params.hitVec);
		
		return true;
	}
	
	/**
	 * Returns everything you need to place a block at the given position, such
	 * as the position of the block to place against (can be a neighbor or the
	 * block itself), the side of that block to place on, the hit vector, the
	 * squared distance to that hit vector, and whether there is line of sight
	 * to that hit vector.
	 */
	public static BlockPlacingParams getBlockPlacingParams(BlockPos pos)
	{
		// if there is a replaceable block at the position, we need to place
		// against the block itself instead of a neighbor
		if(BlockUtils.canBeClicked(pos)
			&& BlockUtils.getState(pos).isReplaceable())
		{
			// the parameters for this happen to be the same as for breaking
			// the block, so we can just use BlockBreaker to get them
			BlockBreakingParams breakParams =
				BlockBreaker.getBlockBreakingParams(pos);
			
			// should never happen, but just in case
			if(breakParams == null)
				return null;
			
			return new BlockPlacingParams(pos, breakParams.side(),
				breakParams.hitVec(), breakParams.distanceSq(),
				breakParams.lineOfSight());
		}
		
		Direction[] sides = Direction.values();
		Vec3d[] hitVecs = new Vec3d[sides.length];
		
		// get hit vectors for all usable sides
		for(int i = 0; i < sides.length; i++)
		{
			BlockPos neighbor = pos.offset(sides[i]);
			BlockState state = BlockUtils.getState(neighbor);
			VoxelShape shape = state.getOutlineShape(MC.world, neighbor);
			
			// if neighbor has no shape or is replaceable, it can't be used
			if(shape.isEmpty() || state.isReplaceable())
				continue;
			
			Box box = shape.getBoundingBox();
			Vec3d halfSize = new Vec3d(box.maxX - box.minX, box.maxY - box.minY,
				box.maxZ - box.minZ).multiply(0.5);
			Vec3d center = Vec3d.of(neighbor).add(box.getCenter());
			
			Vec3i dirVec = sides[i].getOpposite().getVector();
			Vec3d relHitVec = new Vec3d(halfSize.x * dirVec.getX(),
				halfSize.y * dirVec.getY(), halfSize.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		
		double distanceSqToPosVec = eyesPos.squaredDistanceTo(posVec);
		double[] distancesSq = new double[sides.length];
		boolean[] linesOfSight = new boolean[sides.length];
		
		// calculate distances and line of sight
		for(int i = 0; i < sides.length; i++)
		{
			// skip unusable sides
			if(hitVecs[i] == null)
			{
				distancesSq[i] = Double.MAX_VALUE;
				continue;
			}
			
			distancesSq[i] = eyesPos.squaredDistanceTo(hitVecs[i]);
			
			// to place against a neighbor in front of the block, we would
			// have to place against that neighbor's rear face, which can't
			// possibly have line of sight
			if(distancesSq[i] <= distanceSqToPosVec)
				continue;
			
			linesOfSight[i] = BlockUtils.hasLineOfSight(eyesPos, hitVecs[i]);
		}
		
		// decide which side to use
		Direction side = sides[0];
		for(int i = 1; i < sides.length; i++)
		{
			int bestSide = side.ordinal();
			
			// skip unusable sides
			if(hitVecs[i] == null)
				continue;
			
			// prefer sides with LOS
			if(!linesOfSight[bestSide] && linesOfSight[i])
			{
				side = sides[i];
				continue;
			}
			
			if(linesOfSight[bestSide] && !linesOfSight[i])
				continue;
			
			// then pick the furthest side
			if(distancesSq[i] > distancesSq[bestSide])
				side = sides[i];
		}
		
		// if no usable side was found, return null
		if(hitVecs[side.ordinal()] == null)
			return null;
		
		return new BlockPlacingParams(pos.offset(side), side.getOpposite(),
			hitVecs[side.ordinal()], distancesSq[side.ordinal()],
			linesOfSight[side.ordinal()]);
	}
	
	public static record BlockPlacingParams(BlockPos neighbor, Direction side,
		Vec3d hitVec, double distanceSq, boolean lineOfSight)
	{
		public BlockHitResult toHitResult()
		{
			return new BlockHitResult(hitVec, side, neighbor, false);
		}
	}
}
