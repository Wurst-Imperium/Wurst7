/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.Comparator;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.SwingHandSetting.SwingHand;

public enum BlockBreaker
{
	;
	
	private static final WurstClient WURST = WurstClient.INSTANCE;
	private static final Minecraft MC = WurstClient.MC;
	
	public static boolean breakOneBlock(BlockPos pos)
	{
		BlockBreakingParams params = getBlockBreakingParams(pos);
		if(params == null)
			return false;
		
		return breakOneBlock(params);
	}
	
	public static boolean breakOneBlock(BlockBreakingParams params)
	{
		// face block
		WURST.getRotationFaker().faceVectorPacket(params.hitVec);
		
		// damage block
		if(!MC.gameMode.continueDestroyBlock(params.pos, params.side))
			return false;
		
		// swing arm
		SwingHand.SERVER.swing(InteractionHand.MAIN_HAND);
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
	public static BlockBreakingParams getBlockBreakingParams(Vec3 eyes,
		BlockPos pos)
	{
		Direction[] sides = Direction.values();
		
		BlockState state = BlockUtils.getState(pos);
		VoxelShape shape = state.getShape(MC.level, pos);
		if(shape.isEmpty())
			return null;
		
		AABB box = shape.bounds();
		Vec3 halfSize = new Vec3(box.maxX - box.minX, box.maxY - box.minY,
			box.maxZ - box.minZ).scale(0.5);
		Vec3 center = Vec3.atLowerCornerOf(pos).add(box.getCenter());
		
		Vec3[] hitVecs = new Vec3[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getUnitVec3i();
			Vec3 relHitVec = new Vec3(halfSize.x * dirVec.getX(),
				halfSize.y * dirVec.getY(), halfSize.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		double distanceSqToCenter = eyes.distanceToSqr(center);
		double[] distancesSq = new double[sides.length];
		boolean[] linesOfSight = new boolean[sides.length];
		
		for(int i = 0; i < sides.length; i++)
		{
			distancesSq[i] = eyes.distanceToSqr(hitVecs[i]);
			
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
		
		return new BlockBreakingParams(pos, side, hitVecs[side.ordinal()],
			distancesSq[side.ordinal()], linesOfSight[side.ordinal()]);
	}
	
	public static record BlockBreakingParams(BlockPos pos, Direction side,
		Vec3 hitVec, double distanceSq, boolean lineOfSight)
	{
		public BlockHitResult toHitResult()
		{
			return new BlockHitResult(hitVec, side, pos, false);
		}
	}
	
	/**
	 * Returns a comparator that compares BlockBreakingParams by line of sight
	 * first, then by distance.
	 */
	public static Comparator<BlockBreakingParams> comparingParams()
	{
		return Comparator.comparing(BlockBreakingParams::lineOfSight).reversed()
			.thenComparing(params -> params.distanceSq);
	}
	
	/**
	 * Returns a comparator that compares BlockBreakingParams by line of sight
	 * first, then by distance.
	 */
	public static <T> Comparator<T> comparingParams(
		Function<T, BlockBreakingParams> keyExtractor)
	{
		return Comparator.<T, BlockBreakingParams> comparing(keyExtractor,
			comparingParams());
	}
	
	public static void breakBlocksWithPacketSpam(Iterable<BlockPos> blocks)
	{
		Vec3 eyesPos = RotationUtils.getEyesPos();
		ClientPacketListener netHandler = MC.player.connection;
		
		for(BlockPos pos : blocks)
		{
			Vec3 posVec = Vec3.atCenterOf(pos);
			double distanceSqPosVec = eyesPos.distanceToSqr(posVec);
			
			for(Direction side : Direction.values())
			{
				Vec3 hitVec = posVec
					.add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5));
				
				// check if side is facing towards player
				if(eyesPos.distanceToSqr(hitVec) >= distanceSqPosVec)
					continue;
				
				// break block
				netHandler.send(new ServerboundPlayerActionPacket(
					Action.START_DESTROY_BLOCK, pos, side));
				netHandler.send(new ServerboundPlayerActionPacket(
					Action.STOP_DESTROY_BLOCK, pos, side));
				
				break;
			}
		}
	}
}
