/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.IdentifierException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.wurstclient.WurstClient;

public enum BlockUtils
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
	public static BlockState getState(BlockPos pos)
	{
		return MC.level.getBlockState(pos);
	}
	
	public static Block getBlock(BlockPos pos)
	{
		return getState(pos).getBlock();
	}
	
	public static int getId(BlockPos pos)
	{
		return Block.getId(getState(pos));
	}
	
	public static String getName(BlockPos pos)
	{
		return getName(getBlock(pos));
	}
	
	public static String getName(Block block)
	{
		return BuiltInRegistries.BLOCK.getKey(block).toString();
	}
	
	/**
	 * @param name
	 *            a String containing the block's name ({@link Identifier})
	 * @return the requested block, or <code>minecraft:air</code> if the block
	 *         doesn't exist.
	 */
	public static Block getBlockFromName(String name)
	{
		try
		{
			return BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));
			
		}catch(IdentifierException e)
		{
			return Blocks.AIR;
		}
	}
	
	/**
	 * @param nameOrId
	 *            a String containing the block's name ({@link Identifier}) or
	 *            numeric ID.
	 * @return the requested block, or null if the block doesn't exist.
	 */
	public static Block getBlockFromNameOrID(String nameOrId)
	{
		if(MathUtils.isInteger(nameOrId))
		{
			BlockState state =
				Block.BLOCK_STATE_REGISTRY.byId(Integer.parseInt(nameOrId));
			if(state == null)
				return null;
			
			return state.getBlock();
		}
		
		try
		{
			Identifier id = Identifier.parse(nameOrId);
			if(!BuiltInRegistries.BLOCK.containsKey(id))
				return null;
			
			return BuiltInRegistries.BLOCK.getValue(id);
			
		}catch(IdentifierException e)
		{
			return null;
		}
	}
	
	public static float getHardness(BlockPos pos)
	{
		return getState(pos).getDestroyProgress(MC.player, MC.level, pos);
	}
	
	public static boolean isUnbreakable(BlockPos pos)
	{
		return getBlock(pos).defaultDestroyTime() < 0;
	}
	
	private static VoxelShape getOutlineShape(BlockPos pos)
	{
		return getState(pos).getShape(MC.level, pos);
	}
	
	public static AABB getBoundingBox(BlockPos pos)
	{
		return getOutlineShape(pos).bounds().move(pos);
	}
	
	public static boolean canBeClicked(BlockPos pos)
	{
		return getOutlineShape(pos) != Shapes.empty();
	}
	
	public static boolean isOpaqueFullCube(BlockPos pos)
	{
		return getState(pos).isSolidRender();
	}
	
	public static BlockHitResult raycast(Vec3 from, Vec3 to,
		ClipContext.Fluid fluidHandling)
	{
		ClipContext context = new ClipContext(from, to,
			ClipContext.Block.COLLIDER, fluidHandling, MC.player);
		
		return MC.level.clip(context);
	}
	
	public static BlockHitResult raycast(Vec3 from, Vec3 to)
	{
		return raycast(from, to, ClipContext.Fluid.NONE);
	}
	
	public static boolean hasLineOfSight(Vec3 from, Vec3 to)
	{
		return raycast(from, to).getType() == HitResult.Type.MISS;
	}
	
	public static boolean hasLineOfSight(Vec3 to)
	{
		return raycast(RotationUtils.getEyesPos(), to)
			.getType() == HitResult.Type.MISS;
	}
	
	/**
	 * Returns a stream of all blocks that collide with the given box.
	 *
	 * <p>
	 * Unlike {@link CollisionGetter#getBlockCollisions(Entity, AABB)}, this
	 * method
	 * breaks the voxel shapes down into their bounding boxes and only returns
	 * those that actually intersect with the given box. It also assumes that
	 * the entity is the player.
	 */
	public static Stream<AABB> getBlockCollisions(AABB box)
	{
		Iterable<VoxelShape> blockCollisions =
			MC.level.getBlockCollisions(MC.player, box);
		
		return StreamSupport.stream(blockCollisions.spliterator(), false)
			.flatMap(shape -> shape.toAabbs().stream())
			.filter(shapeBox -> shapeBox.intersects(box));
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
	
	public static ArrayList<BlockPos> getAllInBox(BlockPos center, int range)
	{
		return getAllInBox(center.offset(-range, -range, -range),
			center.offset(range, range, range));
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
	
	public static Stream<BlockPos> getAllInBoxStream(BlockPos center, int range)
	{
		return getAllInBoxStream(center.offset(-range, -range, -range),
			center.offset(range, range, range));
	}
}
