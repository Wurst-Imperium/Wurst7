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
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.wurstclient.WurstClient;

public enum BlockBreaker
{
	;
	
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final MinecraftClient MC = WurstClient.MC;
	
	public static boolean breakOneBlock(BlockPos pos)
	{
		BlockBreakingParams params = getBlockBreakingParams(pos);
		if(params == null)
			return false;
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(params.hitVec);
		
		// damage block
		if(!MC.interactionManager.updateBlockBreakingProgress(pos, params.side))
			return false;
		
		// swing arm
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		return true;
	}
	
	/**
	 * Returns everything you need to break a block at the given position, such
	 * as which side to face, the exact hit vector to face that side, the
	 * squared distance to that hit vector, and whether or not there is line of
	 * sight to that hit vector.
	 */
	public static BlockBreakingParams getBlockBreakingParams(BlockPos pos)
	{
		return getBlockBreakingParams(RotationUtils.getEyesPos(), pos);
	}
	
	/**
	 * Returns everything you need to break a block at the given position, such
	 * as which side to face, the exact hit vector to face that side, the
	 * squared distance to that hit vector, and whether or not there is line of
	 * sight to that hit vector.
	 */
	public static BlockBreakingParams getBlockBreakingParams(Vec3d eyes,
		BlockPos pos)
	{
		Direction[] sides = Direction.values();
		
		BlockState state = BlockUtils.getState(pos);
		VoxelShape shape = state.getOutlineShape(MC.world, pos);
		if(shape.isEmpty())
			return null;
		
		Box box = shape.getBoundingBox();
		Vec3d halfSize = new Vec3d(box.maxX - box.minX, box.maxY - box.minY,
			box.maxZ - box.minZ).multiply(0.5);
		Vec3d center = Vec3d.of(pos).add(box.getCenter());
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getVector();
			Vec3d relHitVec = new Vec3d(halfSize.x * dirVec.getX(),
				halfSize.y * dirVec.getY(), halfSize.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		double distanceSqToCenter = eyes.squaredDistanceTo(center);
		double[] distancesSq = new double[sides.length];
		boolean[] linesOfSight = new boolean[sides.length];
		
		for(int i = 0; i < sides.length; i++)
		{
			distancesSq[i] = eyes.squaredDistanceTo(hitVecs[i]);
			
			// no need to raytrace the rear sides,
			// they can't possibly have line of sight
			if(distancesSq[i] >= distanceSqToCenter)
				continue;
			
			linesOfSight[i] = BlockUtils.hasLineOfSight(eyes, hitVecs[i]);
		}
		
		Direction side = sides[0];
		for(int i = 1; i < sides.length; i++)
		{
			int bestSide = side.ordinal();
			
			// prefer sides with LOS
			if(!linesOfSight[bestSide] && linesOfSight[i])
			{
				side = sides[i];
				continue;
			}
			
			if(linesOfSight[bestSide] && !linesOfSight[i])
				continue;
			
			// then pick the closest side
			if(distancesSq[i] < distancesSq[bestSide])
				side = sides[i];
		}
		
		return new BlockBreakingParams(side, hitVecs[side.ordinal()],
			distancesSq[side.ordinal()], linesOfSight[side.ordinal()]);
	}
	
	public static record BlockBreakingParams(Direction side, Vec3d hitVec,
		double distanceSq, boolean lineOfSight)
	{}
	
	public static void breakBlocksWithPacketSpam(Iterable<BlockPos> blocks)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		ClientPlayNetworkHandler netHandler = MC.player.networkHandler;
		
		for(BlockPos pos : blocks)
		{
			Vec3d posVec = Vec3d.ofCenter(pos);
			double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
			
			for(Direction side : Direction.values())
			{
				Vec3d hitVec =
					posVec.add(Vec3d.of(side.getVector()).multiply(0.5));
				
				// check if side is facing towards player
				if(eyesPos.squaredDistanceTo(hitVec) >= distanceSqPosVec)
					continue;
				
				// break block
				netHandler.sendPacket(new PlayerActionC2SPacket(
					Action.START_DESTROY_BLOCK, pos, side));
				netHandler.sendPacket(new PlayerActionC2SPacket(
					Action.STOP_DESTROY_BLOCK, pos, side));
				
				break;
			}
		}
	}
}
