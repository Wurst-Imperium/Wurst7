/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.treebot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
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
	
	public void draw(PoseStack matrixStack)
	{
		int green = 0x8000FF00;
		AABB box = new AABB(BlockPos.ZERO).deflate(1 / 16.0);
		
		AABB stumpBox = box.move(stump);
		RenderUtils.drawCrossBox(matrixStack, stumpBox, green, false);
		
		List<AABB> logBoxes = logs.stream().map(pos -> box.move(pos)).toList();
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
