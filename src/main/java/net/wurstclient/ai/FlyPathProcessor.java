/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import java.util.ArrayList;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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
		BlockPos pos = new BlockPos(MC.player.getPos());
		Vec3d posVec = MC.player.getPos();
		BlockPos nextPos = path.get(index);
		int posIndex = path.indexOf(pos);
		Box nextBox = new Box(nextPos.getX() + 0.3, nextPos.getY(),
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
				Vec3d v = MC.player.getVelocity();
				
				MC.player.setVelocity(v.x / Math.max(Math.abs(v.x) * 50, 1),
					v.y / Math.max(Math.abs(v.y) * 50, 1),
					v.z / Math.max(Math.abs(v.z) * 50, 1));
			}
			
			if(index >= path.size())
				done = true;
			
			return;
		}
		
		lockControls();
		MC.player.abilities.flying = creativeFlying;
		boolean x = posVec.x < nextBox.minX || posVec.x > nextBox.maxX;
		boolean y = posVec.y < nextBox.minY || posVec.y > nextBox.maxY;
		boolean z = posVec.z < nextBox.minZ || posVec.z > nextBox.maxZ;
		boolean horizontal = x || z;
		
		// face next position
		if(horizontal)
		{
			facePosition(nextPos);
			if(Math.abs(MathHelper.wrapDegrees(RotationUtils
				.getHorizontalAngleToLookVec(Vec3d.ofCenter(nextPos)))) > 1)
				return;
		}
		
		// skip mid-air nodes
		Vec3i offset = nextPos.subtract(pos);
		while(index < path.size() - 1
			&& path.get(index).add(offset).equals(path.get(index + 1)))
			index++;
		
		if(creativeFlying)
		{
			Vec3d v = MC.player.getVelocity();
			
			if(!x)
				MC.player.setVelocity(v.x / Math.max(Math.abs(v.x) * 50, 1),
					v.y, v.z);
			if(!y)
				MC.player.setVelocity(v.x,
					v.y / Math.max(Math.abs(v.y) * 50, 1), v.z);
			if(!z)
				MC.player.setVelocity(v.x, v.y,
					v.z / Math.max(Math.abs(v.z) * 50, 1));
		}
		
		Vec3d vecInPos = new Vec3d(nextPos.getX() + 0.5, nextPos.getY() + 0.1,
			nextPos.getZ() + 0.5);
		
		// horizontal movement
		if(horizontal)
		{
			if(!creativeFlying && MC.player.getPos().distanceTo(
				vecInPos) <= WURST.getHax().flightHack.speed.getValue())
			{
				MC.player.updatePosition(vecInPos.x, vecInPos.y, vecInPos.z);
				return;
			}
			
			MC.options.keyForward.setPressed(true);
			
			if(MC.player.horizontalCollision)
				if(posVec.y > nextBox.maxY)
					MC.options.keySneak.setPressed(true);
				else if(posVec.y < nextBox.minY)
					MC.options.keyJump.setPressed(true);
				
			// vertical movement
		}else if(y)
		{
			if(!creativeFlying && MC.player.getPos().distanceTo(
				vecInPos) <= WURST.getHax().flightHack.speed.getValue())
			{
				MC.player.updatePosition(vecInPos.x, vecInPos.y, vecInPos.z);
				return;
			}
			
			if(posVec.y < nextBox.minY)
				MC.options.keyJump.setPressed(true);
			else
				MC.options.keySneak.setPressed(true);
			
			if(MC.player.verticalCollision)
			{
				MC.options.keySneak.setPressed(false);
				MC.options.keyForward.setPressed(true);
			}
		}
	}
}
