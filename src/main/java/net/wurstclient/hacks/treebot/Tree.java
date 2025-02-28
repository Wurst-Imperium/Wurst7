/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.treebot;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.util.RenderUtils;

public class Tree
{
	private final BlockPos stump;
	private final ArrayList<BlockPos> logs;
	
	public Tree(BlockPos stump, ArrayList<BlockPos> logs)
	{
		this.stump = stump;
		this.logs = logs;
	}
	
	public void draw(MatrixStack matrixStack)
	{
		int green = 0x8000FF00;
		Box box = new Box(BlockPos.ORIGIN).contract(1 / 16.0);
		
		Box stumpBox = box.offset(stump);
		RenderUtils.drawCrossBox(matrixStack, stumpBox, green, false);
		
		List<Box> logBoxes = logs.stream().map(pos -> box.offset(pos)).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, logBoxes, green, false);
	}
	
	public BlockPos getStump()
	{
		return stump;
	}
	
	public ArrayList<BlockPos> getLogs()
	{
		return logs;
	}
}
