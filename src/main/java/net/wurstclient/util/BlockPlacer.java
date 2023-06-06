/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.Objects;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
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
			&& BlockUtils.getState(pos).getMaterial().isReplaceable())
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
			if(shape.isEmpty() || state.getMaterial().isReplaceable())
				continue;
			
			Vec3d relCenter = shape.getBoundingBox().getCenter();
			Vec3d center = Vec3d.of(neighbor).add(relCenter);
			
			Vec3i dirVec = sides[i].getOpposite().getVector();
			Vec3d relHitVec = new Vec3d(relCenter.x * dirVec.getX(),
				relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
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
			
			linesOfSight[i] = MC.world
				.raycast(new RaycastContext(eyesPos, hitVecs[i],
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, MC.player))
				.getType() == HitResult.Type.MISS;
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
	
	public static class BlockPlacingParams
	{
		private BlockPos neighbor;
		private Direction side;
		private Vec3d hitVec;
		private double distanceSq;
		private boolean lineOfSight;
		
		public BlockPlacingParams(BlockPos neighbor, Direction side,
			Vec3d hitVec, double distanceSq, boolean lineOfSight)
		{
			this.neighbor = neighbor;
			this.side = side;
			this.hitVec = hitVec;
			this.distanceSq = distanceSq;
			this.lineOfSight = lineOfSight;
		}
		
		public BlockHitResult toHitResult()
		{
			return new BlockHitResult(hitVec, side, neighbor, false);
		}
		
		public BlockPos neighbor()
		{
			return neighbor;
		}
		
		public Direction side()
		{
			return side;
		}
		
		public Vec3d hitVec()
		{
			return hitVec;
		}
		
		public double distanceSq()
		{
			return distanceSq;
		}
		
		public boolean lineOfSight()
		{
			return lineOfSight;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(this == obj)
				return true;
			
			if(!(obj instanceof BlockPlacingParams))
				return false;
			
			BlockPlacingParams other = (BlockPlacingParams)obj;
			return Objects.equals(neighbor, other.neighbor)
				&& side == other.side && Objects.equals(hitVec, other.hitVec)
				&& Double.compare(distanceSq, other.distanceSq) == 0
				&& lineOfSight == other.lineOfSight;
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(neighbor, side, hitVec, distanceSq,
				lineOfSight);
		}
		
		@Override
		public String toString()
		{
			return "BlockBreakingParams{neighbor=" + neighbor + ", side=" + side
				+ ", hitVec=" + hitVec + ", distanceSq=" + distanceSq
				+ ", lineOfSight=" + lineOfSight + '}';
		}
	}
}
