/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.util.BlockUtils;

public final class Area
{
	private final Box box;
	
	private final int totalBlocks, scanSpeed;
	private final Iterator<BlockPos> iterator;
	
	private int scannedBlocks;
	private float progress;
	
	private final ArrayList<BlockPos> blocksFound = new ArrayList<>();
	
	public Area(BlockPos start, BlockPos end)
	{
		box = Box.enclosing(start, end);
		int lengthX = Math.abs(start.getX() - end.getX()) + 1;
		int lengthY = Math.abs(start.getY() - end.getY()) + 1;
		int lengthZ = Math.abs(start.getZ() - end.getZ()) + 1;
		totalBlocks = lengthX * lengthY * lengthZ;
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
	
	public Box toBox()
	{
		return box;
	}
}
