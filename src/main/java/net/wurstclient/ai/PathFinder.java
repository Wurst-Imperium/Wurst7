/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.WurstClient;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;

public class PathFinder
{
	private final WurstClient wurst = WurstClient.INSTANCE;
	
	private final boolean invulnerable =
		WurstClient.MC.player.abilities.creativeMode;
	private final boolean creativeFlying =
		WurstClient.MC.player.abilities.flying;
	protected final boolean flying =
		creativeFlying || wurst.getHax().flightHack.isEnabled();
	private final boolean immuneToFallDamage =
		invulnerable || wurst.getHax().noFallHack.isEnabled();
	private final boolean noWaterSlowdown = false;
	// TODO: wurst.getHax().noSlowdownHack.blockWaterSlowness();
	private final boolean jesus = wurst.getHax().jesusHack.isEnabled();
	private final boolean spider = wurst.getHax().spiderHack.isEnabled();
	protected boolean fallingAllowed = true;
	protected boolean divingAllowed = true;
	
	private final PathPos start;
	protected PathPos current;
	private final BlockPos goal;
	
	private final HashMap<PathPos, Float> costMap = new HashMap<>();
	protected final HashMap<PathPos, PathPos> prevPosMap = new HashMap<>();
	private final PathQueue queue = new PathQueue();
	
	protected int thinkSpeed = 1024;
	protected int thinkTime = 200;
	private int iterations;
	
	protected boolean done;
	protected boolean failed;
	private final ArrayList<PathPos> path = new ArrayList<>();
	
	public PathFinder(BlockPos goal)
	{
		if(WurstClient.MC.player.isOnGround())
			start = new PathPos(new BlockPos(WurstClient.MC.player.getX(),
				WurstClient.MC.player.getY() + 0.5,
				WurstClient.MC.player.getZ()));
		else
			start = new PathPos(new BlockPos(WurstClient.MC.player.getPos()));
		this.goal = goal;
		
		costMap.put(start, 0F);
		queue.add(start, getHeuristic(start));
	}
	
	public PathFinder(PathFinder pathFinder)
	{
		this(pathFinder.goal);
		thinkSpeed = pathFinder.thinkSpeed;
		thinkTime = pathFinder.thinkTime;
	}
	
	public void think()
	{
		if(done)
			throw new IllegalStateException("Path was already found!");
		
		int i = 0;
		for(; i < thinkSpeed && !checkFailed(); i++)
		{
			// get next position from queue
			current = queue.poll();
			
			// check if path is found
			if(checkDone())
				return;
			
			// add neighbors to queue
			for(PathPos next : getNeighbors(current))
			{
				// check cost
				float newCost = costMap.get(current) + getCost(current, next);
				if(costMap.containsKey(next) && costMap.get(next) <= newCost)
					continue;
				
				// add to queue
				costMap.put(next, newCost);
				prevPosMap.put(next, current);
				queue.add(next, newCost + getHeuristic(next));
			}
		}
		iterations += i;
	}
	
	protected boolean checkDone()
	{
		return done = goal.equals(current);
	}
	
	private boolean checkFailed()
	{
		return failed = queue.isEmpty() || iterations >= thinkSpeed * thinkTime;
	}
	
	private ArrayList<PathPos> getNeighbors(PathPos pos)
	{
		ArrayList<PathPos> neighbors = new ArrayList<>();
		
		// abort if too far away
		if(Math.abs(start.getX() - pos.getX()) > 256
			|| Math.abs(start.getZ() - pos.getZ()) > 256)
			return neighbors;
		
		// get all neighbors
		BlockPos north = pos.north();
		BlockPos east = pos.east();
		BlockPos south = pos.south();
		BlockPos west = pos.west();
		
		BlockPos northEast = north.east();
		BlockPos southEast = south.east();
		BlockPos southWest = south.west();
		BlockPos northWest = north.west();
		
		BlockPos up = pos.up();
		BlockPos down = pos.down();
		
		// flying
		boolean flying = canFlyAt(pos);
		// walking
		boolean onGround = canBeSolid(down);
		
		// player can move sideways if flying, standing on the ground, jumping,
		// or inside of a block that allows sideways movement (ladders, webs,
		// etc.)
		if(flying || onGround || pos.isJumping()
			|| canMoveSidewaysInMidairAt(pos) || canClimbUpAt(pos.down()))
		{
			// north
			if(checkHorizontalMovement(pos, north))
				neighbors.add(new PathPos(north));
			
			// east
			if(checkHorizontalMovement(pos, east))
				neighbors.add(new PathPos(east));
			
			// south
			if(checkHorizontalMovement(pos, south))
				neighbors.add(new PathPos(south));
			
			// west
			if(checkHorizontalMovement(pos, west))
				neighbors.add(new PathPos(west));
			
			// north-east
			if(checkDiagonalMovement(pos, Direction.NORTH, Direction.EAST))
				neighbors.add(new PathPos(northEast));
			
			// south-east
			if(checkDiagonalMovement(pos, Direction.SOUTH, Direction.EAST))
				neighbors.add(new PathPos(southEast));
			
			// south-west
			if(checkDiagonalMovement(pos, Direction.SOUTH, Direction.WEST))
				neighbors.add(new PathPos(southWest));
			
			// north-west
			if(checkDiagonalMovement(pos, Direction.NORTH, Direction.WEST))
				neighbors.add(new PathPos(northWest));
		}
		
		// up
		if(pos.getY() < 256 && canGoThrough(up.up())
			&& (flying || onGround || canClimbUpAt(pos))
			&& (flying || canClimbUpAt(pos) || goal.equals(up)
				|| canSafelyStandOn(north) || canSafelyStandOn(east)
				|| canSafelyStandOn(south) || canSafelyStandOn(west))
			&& (divingAllowed || BlockUtils.getState(up.up())
				.getMaterial() != Material.WATER))
			neighbors.add(new PathPos(up, onGround));
		
		// down
		if(pos.getY() > 0 && canGoThrough(down) && canGoAbove(down.down())
			&& (flying || canFallBelow(pos)) && (divingAllowed
				|| BlockUtils.getState(pos).getMaterial() != Material.WATER))
			neighbors.add(new PathPos(down));
		
		return neighbors;
	}
	
	private boolean checkHorizontalMovement(BlockPos current, BlockPos next)
	{
		if(isPassable(next) && (canFlyAt(current) || canGoThrough(next.down())
			|| canSafelyStandOn(next.down())))
			return true;
		
		return false;
	}
	
	private boolean checkDiagonalMovement(BlockPos current,
		Direction direction1, Direction direction2)
	{
		BlockPos horizontal1 = current.offset(direction1);
		BlockPos horizontal2 = current.offset(direction2);
		BlockPos next = horizontal1.offset(direction2);
		
		if(isPassable(horizontal1) && isPassable(horizontal2)
			&& checkHorizontalMovement(current, next))
			return true;
		
		return false;
	}
	
	protected boolean isPassable(BlockPos pos)
	{
		return canGoThrough(pos) && canGoThrough(pos.up())
			&& canGoAbove(pos.down()) && (divingAllowed || BlockUtils
				.getState(pos.up()).getMaterial() != Material.WATER);
	}
	
	protected boolean canBeSolid(BlockPos pos)
	{
		Material material = BlockUtils.getState(pos).getMaterial();
		Block block = BlockUtils.getBlock(pos);
		return material.blocksMovement()
			&& !(block instanceof AbstractSignBlock)
			|| block instanceof LadderBlock || jesus
				&& (material == Material.WATER || material == Material.LAVA);
	}
	
	private boolean canGoThrough(BlockPos pos)
	{
		// check if loaded
		if(!WurstClient.MC.world.isChunkLoaded(pos))
			return false;
		
		// check if solid
		Material material = BlockUtils.getState(pos).getMaterial();
		Block block = BlockUtils.getBlock(pos);
		if(material.blocksMovement() && !(block instanceof AbstractSignBlock))
			return false;
		
		// check if trapped
		if(block instanceof TripwireBlock
			|| block instanceof PressurePlateBlock)
			return false;
		
		// check if safe
		if(!invulnerable
			&& (material == Material.LAVA || material == Material.FIRE))
			return false;
		
		return true;
	}
	
	private boolean canGoAbove(BlockPos pos)
	{
		// check for fences, etc.
		Block block = BlockUtils.getBlock(pos);
		if(block instanceof FenceBlock || block instanceof WallBlock
			|| block instanceof FenceGateBlock)
			return false;
		
		return true;
	}
	
	private boolean canSafelyStandOn(BlockPos pos)
	{
		// check if solid
		Material material = BlockUtils.getState(pos).getMaterial();
		if(!canBeSolid(pos))
			return false;
		
		// check if safe
		if(!invulnerable
			&& (material == Material.CACTUS || material == Material.LAVA))
			return false;
		
		return true;
	}
	
	private boolean canFallBelow(PathPos pos)
	{
		// check if player can keep falling
		BlockPos down2 = pos.down(2);
		if(fallingAllowed && canGoThrough(down2))
			return true;
		
		// check if player can stand below
		if(!canSafelyStandOn(down2))
			return false;
		
		// check if fall damage is off
		if(immuneToFallDamage && fallingAllowed)
			return true;
		
		// check if fall ends with slime block
		if(BlockUtils.getBlock(down2) instanceof SlimeBlock && fallingAllowed)
			return true;
		
		// check fall damage
		BlockPos prevPos = pos;
		for(int i = 0; i <= (fallingAllowed ? 3 : 1); i++)
		{
			// check if prevPos does not exist, meaning that the pathfinding
			// started during the fall and fall damage should be ignored because
			// it cannot be prevented
			if(prevPos == null)
				return true;
				
			// check if point is not part of this fall, meaning that the fall is
			// too short to cause any damage
			if(!pos.up(i).equals(prevPos))
				return true;
			
			// check if block resets fall damage
			Block prevBlock = BlockUtils.getBlock(prevPos);
			BlockState prevState = BlockUtils.getState(prevPos);
			if(prevState.getMaterial() == Material.WATER
				|| prevBlock instanceof LadderBlock
				|| prevBlock instanceof VineBlock
				|| prevBlock instanceof CobwebBlock)
				return true;
			
			prevPos = prevPosMap.get(prevPos);
		}
		
		return false;
	}
	
	private boolean canFlyAt(BlockPos pos)
	{
		return flying || !noWaterSlowdown
			&& BlockUtils.getState(pos).getMaterial() == Material.WATER;
	}
	
	private boolean canClimbUpAt(BlockPos pos)
	{
		// check if this block works for climbing
		Block block = BlockUtils.getBlock(pos);
		if(!spider && !(block instanceof LadderBlock)
			&& !(block instanceof VineBlock))
			return false;
		
		// check if any adjacent block is solid
		BlockPos up = pos.up();
		if(!canBeSolid(pos.north()) && !canBeSolid(pos.east())
			&& !canBeSolid(pos.south()) && !canBeSolid(pos.west())
			&& !canBeSolid(up.north()) && !canBeSolid(up.east())
			&& !canBeSolid(up.south()) && !canBeSolid(up.west()))
			return false;
		
		return true;
	}
	
	private boolean canMoveSidewaysInMidairAt(BlockPos pos)
	{
		// check feet
		Block blockFeet = BlockUtils.getBlock(pos);
		if(BlockUtils.getState(pos).getMaterial().isLiquid()
			|| blockFeet instanceof LadderBlock
			|| blockFeet instanceof VineBlock
			|| blockFeet instanceof CobwebBlock)
			return true;
		
		// check head
		Block blockHead = BlockUtils.getBlock(pos.up());
		if(BlockUtils.getState(pos.up()).getMaterial().isLiquid()
			|| blockHead instanceof CobwebBlock)
			return true;
		
		return false;
	}
	
	private float getCost(BlockPos current, BlockPos next)
	{
		float[] costs = {0.5F, 0.5F};
		BlockPos[] positions = new BlockPos[]{current, next};
		
		for(int i = 0; i < positions.length; i++)
		{
			Material material = BlockUtils.getState(positions[i]).getMaterial();
			
			// liquids
			if(material == Material.WATER && !noWaterSlowdown)
				costs[i] *= 1.3164437838225804F;
			else if(material == Material.LAVA)
				costs[i] *= 4.539515393656079F;
			
			// soul sand
			if(!canFlyAt(positions[i]) && BlockUtils
				.getBlock(positions[i].down()) instanceof SoulSandBlock)
				costs[i] *= 2.5F;
		}
		
		float cost = costs[0] + costs[1];
		
		// diagonal movement
		if(current.getX() != next.getX() && current.getZ() != next.getZ())
			cost *= 1.4142135623730951F;
		
		return cost;
	}
	
	private float getHeuristic(BlockPos pos)
	{
		float dx = Math.abs(pos.getX() - goal.getX());
		float dy = Math.abs(pos.getY() - goal.getY());
		float dz = Math.abs(pos.getZ() - goal.getZ());
		return 1.001F * (dx + dy + dz - 0.5857864376269049F * Math.min(dx, dz));
	}
	
	public PathPos getCurrentPos()
	{
		return current;
	}
	
	public BlockPos getGoal()
	{
		return goal;
	}
	
	public int countProcessedBlocks()
	{
		return prevPosMap.keySet().size();
	}
	
	public int getQueueSize()
	{
		return queue.size();
	}
	
	public float getCost(BlockPos pos)
	{
		return costMap.get(pos);
	}
	
	public boolean isDone()
	{
		return done;
	}
	
	public boolean isFailed()
	{
		return failed;
	}
	
	public ArrayList<PathPos> formatPath()
	{
		if(!done && !failed)
			throw new IllegalStateException("No path found!");
		if(!path.isEmpty())
			throw new IllegalStateException("Path was already formatted!");
		
		// get last position
		PathPos pos;
		if(!failed)
			pos = current;
		else
		{
			pos = start;
			for(PathPos next : prevPosMap.keySet())
				if(getHeuristic(next) < getHeuristic(pos)
					&& (canFlyAt(next) || canBeSolid(next.down())))
					pos = next;
		}
		
		// get positions
		while(pos != null)
		{
			path.add(pos);
			pos = prevPosMap.get(pos);
		}
		
		// reverse path
		Collections.reverse(path);
		
		return path;
	}
	
	public void renderPath(boolean debugMode, boolean depthTest)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		if(!depthTest)
			GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glDepthMask(false);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		GL11.glTranslated(0.5, 0.5, 0.5);
		
		if(debugMode)
		{
			int renderedThings = 0;
			
			// queue (yellow)
			GL11.glLineWidth(2);
			GL11.glColor4f(1, 1, 0, 0.75F);
			for(PathPos element : queue.toArray())
			{
				if(renderedThings >= 5000)
					break;
				
				PathRenderer.renderNode(element);
				renderedThings++;
			}
			
			// processed (red)
			GL11.glLineWidth(2);
			for(Entry<PathPos, PathPos> entry : prevPosMap.entrySet())
			{
				if(renderedThings >= 5000)
					break;
				
				if(entry.getKey().isJumping())
					GL11.glColor4f(1, 0, 1, 0.75F);
				else
					GL11.glColor4f(1, 0, 0, 0.75F);
				
				PathRenderer.renderArrow(entry.getValue(), entry.getKey());
				renderedThings++;
			}
		}
		
		// path (blue)
		if(debugMode)
		{
			GL11.glLineWidth(4);
			GL11.glColor4f(0, 0, 1, 0.75F);
		}else
		{
			GL11.glLineWidth(2);
			GL11.glColor4f(0, 1, 0, 0.75F);
		}
		for(int i = 0; i < path.size() - 1; i++)
			PathRenderer.renderArrow(path.get(i), path.get(i + 1));
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
	}
	
	public boolean isPathStillValid(int index)
	{
		if(path.isEmpty())
			throw new IllegalStateException("Path is not formatted!");
		
		// check player abilities
		if(invulnerable != WurstClient.MC.player.abilities.creativeMode
			|| flying != (creativeFlying
				|| wurst.getHax().flightHack.isEnabled())
			|| immuneToFallDamage != (invulnerable
				|| wurst.getHax().noFallHack.isEnabled())
			// TODO:
			// || noWaterSlowdown !=
			// wurst.getHax().noSlowdownHack.blockWaterSlowness()
			|| jesus != wurst.getHax().jesusHack.isEnabled()
			|| spider != wurst.getHax().spiderHack.isEnabled())
			return false;
		
		// if index is zero, check if first pos is safe
		if(index == 0)
		{
			PathPos pos = path.get(0);
			if(!isPassable(pos) || !canFlyAt(pos) && !canGoThrough(pos.down())
				&& !canSafelyStandOn(pos.down()))
				return false;
		}
		
		// check path
		for(int i = Math.max(1, index); i < path.size(); i++)
			if(!getNeighbors(path.get(i - 1)).contains(path.get(i)))
				return false;
			
		return true;
	}
	
	public PathProcessor getProcessor()
	{
		if(flying)
			return new FlyPathProcessor(path, creativeFlying);
		
		return new WalkPathProcessor(path);
	}
	
	public void setThinkSpeed(int thinkSpeed)
	{
		this.thinkSpeed = thinkSpeed;
	}
	
	public void setThinkTime(int thinkTime)
	{
		this.thinkTime = thinkTime;
	}
	
	public void setFallingAllowed(boolean fallingAllowed)
	{
		this.fallingAllowed = fallingAllowed;
	}
	
	public void setDivingAllowed(boolean divingAllowed)
	{
		this.divingAllowed = divingAllowed;
	}
}
