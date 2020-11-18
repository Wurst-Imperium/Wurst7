/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import net.minecraft.util.math.BlockPos;

public class PathPos extends BlockPos
{
	private final boolean jumping;
	
	public PathPos(BlockPos pos)
	{
		this(pos, false);
	}
	
	public PathPos(BlockPos pos, boolean jumping)
	{
		super(pos);
		this.jumping = jumping;
	}
	
	public boolean isJumping()
	{
		return jumping;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		
		if(!(obj instanceof PathPos))
			return false;
		
		PathPos node = (PathPos)obj;
		return getX() == node.getX() && getY() == node.getY()
			&& getZ() == node.getZ() && isJumping() == node.isJumping();
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode() * 2 + (isJumping() ? 1 : 0);
	}
}
