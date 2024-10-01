/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.util.BlockUtils;

public final class Area
{
	private final int minX, minY, minZ;
	private final int sizeX, sizeY, sizeZ;
	
	private final int totalBlocks, scanSpeed;
	private final Iterator<BlockPos> iterator;
	
	private int scannedBlocks;
	private float progress;
	
	private final ArrayList<BlockPos> blocksFound = new ArrayList<>();
	
	public Area(BlockPos start, BlockPos end)
	{
		int startX = start.getX();
		int startY = start.getY();
		int startZ = start.getZ();
		
		int endX = end.getX();
		int endY = end.getY();
		int endZ = end.getZ();
		
		minX = Math.min(startX, endX);
		minY = Math.min(startY, endY);
		minZ = Math.min(startZ, endZ);
		
		sizeX = Math.abs(startX - endX);
		sizeY = Math.abs(startY - endY);
		sizeZ = Math.abs(startZ - endZ);
		
		totalBlocks = (sizeX + 1) * (sizeY + 1) * (sizeZ + 1);
		scanSpeed = MathHelper.clamp(totalBlocks / 30, 1, 1024);
		iterator = BlockUtils.getAllInBox(start, end).iterator();
	}
	
	public int getScannedBlocks()
	{
		return scannedBlocks;
	}
	
	public void setScannedBlocks(int scannedBlocks)
	{
		this.scannedBlocks = scannedBlocks;
	}
	
	public float getProgress()
	{
		return progress;
	}
	
	public void setProgress(float progress)
	{
		this.progress = progress;
	}
	
	public int getMinX()
	{
		return minX;
	}
	
	public int getMinY()
	{
		return minY;
	}
	
	public int getMinZ()
	{
		return minZ;
	}
	
	public int getSizeX()
	{
		return sizeX;
	}
	
	public int getSizeY()
	{
		return sizeY;
	}
	
	public int getSizeZ()
	{
		return sizeZ;
	}
	
	public int getTotalBlocks()
	{
		return totalBlocks;
	}
	
	public int getScanSpeed()
	{
		return scanSpeed;
	}
	
	public Iterator<BlockPos> getIterator()
	{
		return iterator;
	}
	
	public ArrayList<BlockPos> getBlocksFound()
	{
		return blocksFound;
	}
}
