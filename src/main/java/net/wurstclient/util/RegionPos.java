/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public record RegionPos(int x, int z)
{
	public static RegionPos of(BlockPos pos)
	{
		return new RegionPos(pos.getX() >> 9 << 9, pos.getZ() >> 9 << 9);
	}
	
	public static RegionPos of(ChunkPos pos)
	{
		return new RegionPos(pos.x >> 5 << 9, pos.z >> 5 << 9);
	}
	
	public RegionPos negate()
	{
		return new RegionPos(-x, -z);
	}
	
	public Vec3d toVec3d()
	{
		return new Vec3d(x, 0, z);
	}
	
	public BlockPos toBlockPos()
	{
		return new BlockPos(x, 0, z);
	}
}
