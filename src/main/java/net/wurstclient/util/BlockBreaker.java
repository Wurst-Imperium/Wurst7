/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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
		Direction side = null;
		Direction[] sides = Direction.values();
		
		BlockState state = BlockUtils.getState(pos);
		VoxelShape shape = state.getOutlineShape(MC.world, pos);
		if(shape.isEmpty())
			return false;
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d relCenter = shape.getBoundingBox().getCenter();
		Vec3d center = Vec3d.of(pos).add(relCenter);
		
		Vec3d[] hitVecs = new Vec3d[sides.length];
		for(int i = 0; i < sides.length; i++)
		{
			Vec3i dirVec = sides[i].getVector();
			Vec3d relHitVec = new Vec3d(relCenter.x * dirVec.getX(),
				relCenter.y * dirVec.getY(), relCenter.z * dirVec.getZ());
			hitVecs[i] = center.add(relHitVec);
		}
		
		for(int i = 0; i < sides.length; i++)
		{
			// check line of sight
			if(MC.world.raycastBlock(eyesPos, hitVecs[i], pos, shape,
				state) != null)
				continue;
			
			side = sides[i];
			break;
		}
		
		if(side == null)
		{
			double distanceSqToCenter = eyesPos.squaredDistanceTo(center);
			for(int i = 0; i < sides.length; i++)
			{
				// check if side is facing towards player
				if(eyesPos.squaredDistanceTo(hitVecs[i]) >= distanceSqToCenter)
					continue;
				
				side = sides[i];
				break;
			}
		}
		
		// player is inside of block, side doesn't matter
		if(side == null)
			side = sides[0];
		
		// face block
		WURST.getRotationFaker().faceVectorPacket(hitVecs[side.ordinal()]);
		
		// damage block
		if(!MC.interactionManager.updateBlockBreakingProgress(pos, side))
			return false;
		
		// swing arm
		MC.player.networkHandler
			.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
		
		return true;
	}
	
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
