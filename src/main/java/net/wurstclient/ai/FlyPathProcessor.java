/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import java.util.ArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.util.RotationUtils;

public class FlyPathProcessor extends PathProcessor
{
	private final boolean creativeFlying;
	
	public FlyPathProcessor(ArrayList<PathPos> path, boolean creativeFlying)
	{
		super(path);
		this.creativeFlying = creativeFlying;
	}
	
	@Override
	public void process()
	{
		// get positions
		BlockPos pos = BlockPos.containing(MC.player.position());
		Vec3 posVec = MC.player.position();
		BlockPos nextPos = path.get(index);
		int posIndex = path.indexOf(pos);
		AABB nextBox = new AABB(nextPos.getX() + 0.3, nextPos.getY(),
			nextPos.getZ() + 0.3, nextPos.getX() + 0.7, nextPos.getY() + 0.2,
			nextPos.getZ() + 0.7);
		
		if(posIndex == -1)
			ticksOffPath++;
		else
			ticksOffPath = 0;
		
		// update index
		if(posIndex > index
			|| posVec.x >= nextBox.minX && posVec.x <= nextBox.maxX
				&& posVec.y >= nextBox.minY && posVec.y <= nextBox.maxY
				&& posVec.z >= nextBox.minZ && posVec.z <= nextBox.maxZ)
		{
			if(posIndex > index)
				index = posIndex + 1;
			else
				index++;
			
			// stop when changing directions
			if(creativeFlying)
			{
				Vec3 v = MC.player.getDeltaMovement();
				
				MC.player.setDeltaMovement(
					v.x / Math.max(Math.abs(v.x) * 50, 1),
					v.y / Math.max(Math.abs(v.y) * 50, 1),
					v.z / Math.max(Math.abs(v.z) * 50, 1));
			}
			
			if(index >= path.size())
				done = true;
			
			return;
		}
		
		lockControls();
		MC.player.getAbilities().flying = creativeFlying;
		boolean x = posVec.x < nextBox.minX || posVec.x > nextBox.maxX;
		boolean y = posVec.y < nextBox.minY || posVec.y > nextBox.maxY;
		boolean z = posVec.z < nextBox.minZ || posVec.z > nextBox.maxZ;
		boolean horizontal = x || z;
		
		// face next position
		if(horizontal)
		{
			facePosition(nextPos);
			if(Math.abs(Mth.wrapDegrees(RotationUtils
				.getHorizontalAngleToLookVec(Vec3.atCenterOf(nextPos)))) > 1)
				return;
		}
		
		// skip mid-air nodes
		Vec3i offset = nextPos.subtract(pos);
		while(index < path.size() - 1
			&& path.get(index).offset(offset).equals(path.get(index + 1)))
			index++;
		
		if(creativeFlying)
		{
			Vec3 v = MC.player.getDeltaMovement();
			
			if(!x)
				MC.player.setDeltaMovement(
					v.x / Math.max(Math.abs(v.x) * 50, 1), v.y, v.z);
			if(!y)
				MC.player.setDeltaMovement(v.x,
					v.y / Math.max(Math.abs(v.y) * 50, 1), v.z);
			if(!z)
				MC.player.setDeltaMovement(v.x, v.y,
					v.z / Math.max(Math.abs(v.z) * 50, 1));
		}
		
		Vec3 vecInPos = new Vec3(nextPos.getX() + 0.5, nextPos.getY() + 0.1,
			nextPos.getZ() + 0.5);
		
		// horizontal movement
		if(horizontal)
		{
			if(!creativeFlying && MC.player.position().distanceTo(
				vecInPos) <= WURST.getHax().flightHack.horizontalSpeed
					.getValue())
			{
				MC.player.setPos(vecInPos.x, vecInPos.y, vecInPos.z);
				return;
			}
			
			MC.options.keyUp.setDown(true);
			
			if(MC.player.horizontalCollision)
				if(posVec.y > nextBox.maxY)
					MC.options.keyShift.setDown(true);
				else if(posVec.y < nextBox.minY)
					MC.options.keyJump.setDown(true);
				
			// vertical movement
		}else if(y)
		{
			if(!creativeFlying && MC.player.position().distanceTo(
				vecInPos) <= WURST.getHax().flightHack.verticalSpeed.getValue())
			{
				MC.player.setPos(vecInPos.x, vecInPos.y, vecInPos.z);
				return;
			}
			
			if(posVec.y < nextBox.minY)
				MC.options.keyJump.setDown(true);
			else
				MC.options.keyShift.setDown(true);
			
			if(MC.player.verticalCollision)
			{
				MC.options.keyShift.setDown(false);
				MC.options.keyUp.setDown(true);
			}
		}
	}
	
	@Override
	public boolean canBreakBlocks()
	{
		return true;
	}
}
