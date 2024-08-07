/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import java.util.LinkedHashSet;
import java.util.TreeSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public final class Template
{
	private final int totalBlocks, scanSpeed;
	private float progress;
	
	private final TreeSet<BlockPos> remainingBlocks;
	private final LinkedHashSet<BlockPos> sortedBlocks = new LinkedHashSet<>();
	private BlockPos lastAddedBlock;
	
	public Template(BlockPos firstBlock, int blocksFound)
	{
		totalBlocks = blocksFound;
		scanSpeed = MathHelper.clamp(blocksFound / 15, 1, 1024);
		
		remainingBlocks = new TreeSet<>((o1, o2) -> {
			
			// compare distance to start pos
			int distanceDiff = Double.compare(o1.getSquaredDistance(firstBlock),
				o2.getSquaredDistance(firstBlock));
			if(distanceDiff != 0)
				return distanceDiff;
			
			// fallback in case of same distance
			return o1.compareTo(o2);
		});
	}
	
	public float getProgress()
	{
		return progress;
	}
	
	public void setProgress(float progress)
	{
		this.progress = progress;
	}
	
	public BlockPos getLastAddedBlock()
	{
		return lastAddedBlock;
	}
	
	public void setLastAddedBlock(BlockPos lastAddedBlock)
	{
		this.lastAddedBlock = lastAddedBlock;
	}
	
	public int getTotalBlocks()
	{
		return totalBlocks;
	}
	
	public int getScanSpeed()
	{
		return scanSpeed;
	}
	
	public TreeSet<BlockPos> getRemainingBlocks()
	{
		return remainingBlocks;
	}
	
	public LinkedHashSet<BlockPos> getSortedBlocks()
	{
		return sortedBlocks;
	}
}
