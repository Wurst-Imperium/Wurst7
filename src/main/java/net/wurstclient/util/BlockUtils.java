/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.wurstclient.WurstClient;

import static net.wurstclient.WurstClient.IMC;

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
	
	public static Stream<BlockPos> getAllInBoxStream(BlockPos from, BlockPos to)
	{
		BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()),
			Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
		BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()),
			Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
		
		Stream<BlockPos> stream = Stream.<BlockPos> iterate(min, pos -> {
			
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			
			x++;
			
			if(x > max.getX())
			{
				x = min.getX();
				y++;
			}
			
			if(y > max.getY())
			{
				y = min.getY();
				z++;
			}
			
			if(z > max.getZ())
				throw new IllegalStateException("Stream limit didn't work.");
			
			return new BlockPos(x, y, z);
		});
		
		int limit = (max.getX() - min.getX() + 1)
			* (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
		
		return stream.limit(limit);
	}

	private static boolean hasLineOfSight(Vec3d from, Vec3d to)
	{
		RaycastContext.ShapeType type = RaycastContext.ShapeType.COLLIDER;
		RaycastContext.FluidHandling fluid = RaycastContext.FluidHandling.NONE;

		RaycastContext context =
				new RaycastContext(from, to, type, fluid, MC.player);

		return MC.world.raycast(context).getType() == HitResult.Type.MISS;
	}

	public static boolean rightClickBlockLegit(BlockPos pos, boolean checkLOS, boolean interactWithItem, int range)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		double rangeSq = Math.pow(range, 2);

		for(Direction side : Direction.values())
		{
			Vec3d hitVec = posVec.add(Vec3d.of(side.getVector()).multiply(0.5));
			double distanceSqHitVec = eyesPos.squaredDistanceTo(hitVec);

			// check if hitVec is within range
			if(distanceSqHitVec > rangeSq)
				continue;

			// check if side is facing towards player
			if(distanceSqHitVec >= distanceSqPosVec)
				continue;

			if(checkLOS && !hasLineOfSight(eyesPos, hitVec))
				continue;

			// face block
			WurstClient.INSTANCE.getRotationFaker().faceVectorPacket(hitVec);

			// right click block
			if (interactWithItem) {
				IMC.getInteractionManager().rightClickBlock(pos, side, hitVec);
			}
			else {
				IMC.getInteractionManager().rightClickBlockNoItem(pos, side, hitVec);
			}
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);
			return true;
		}

		return false;
	}

	public static boolean rightClickBlockSimple(BlockPos pos, boolean checkLOS, int range)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		double rangeSq = Math.pow(range, 2);

		for(Direction side : Direction.values())
		{
			Vec3d hitVec = posVec.add(Vec3d.of(side.getVector()).multiply(0.5));
			double distanceSqHitVec = eyesPos.squaredDistanceTo(hitVec);

			// check if hitVec is within range
			if(distanceSqHitVec > rangeSq)
				continue;

			// check if side is facing towards player
			if(distanceSqHitVec >= distanceSqPosVec)
				continue;

			if(checkLOS && !hasLineOfSight(eyesPos, hitVec))
				continue;

			IMC.getInteractionManager().rightClickBlock(pos, side, hitVec);
			return true;
		}

		return false;
	}
}
