/*
 * Copyright (C) 2021 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.treebot;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.util.RenderUtils;

public class Tree implements AutoCloseable
{
	private final BlockPos stump;
	private final ArrayList<BlockPos> logs;
	private int displayList;
	
	public Tree(BlockPos stump, ArrayList<BlockPos> logs)
	{
		this.stump = stump;
		this.logs = logs;
		compileBuffer();
	}
	
	public void compileBuffer()
	{
		if(displayList != 0)
			GL11.glDeleteLists(displayList, 1);
		
		displayList = GL11.glGenLists(1);
		
		int regionX = (stump.getX() >> 9) * 512;
		int regionZ = (stump.getZ() >> 9) * 512;
		
		double boxMin = 1 / 16.0;
		double boxMax = 15 / 16.0;
		Box box = new Box(boxMin, boxMin, boxMin, boxMax, boxMax, boxMax);
		
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		
		RenderUtils
			.drawCrossBox(box.offset(stump).offset(-regionX, 0, -regionZ));
		
		for(BlockPos log : logs)
			RenderUtils
				.drawOutlinedBox(box.offset(log).offset(-regionX, 0, -regionZ));
		
		GL11.glEndList();
	}
	
	@Override
	public void close()
	{
		if(displayList == 0)
			return;
		
		GL11.glDeleteLists(displayList, 1);
		displayList = 0;
	}
	
	public BlockPos getStump()
	{
		return stump;
	}
	
	public ArrayList<BlockPos> getLogs()
	{
		return logs;
	}
	
	public int getDisplayList()
	{
		return displayList;
	}
}
