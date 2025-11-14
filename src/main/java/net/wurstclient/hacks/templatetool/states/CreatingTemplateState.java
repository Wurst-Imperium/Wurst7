/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.TemplateToolState;
import net.wurstclient.util.RenderUtils;

public final class CreatingTemplateState extends TemplateToolState
{
	private int totalBlocks;
	private int blocksPerTick;
	private float progress;
	private ArrayDeque<BlockPos> unsortedBlocks;
	private TreeSet<BlockPos> sortingHelper;
	
	@Override
	public void onEnter(TemplateToolHack hack)
	{
		totalBlocks = hack.getNonEmptyBlocks().size();
		blocksPerTick = Mth.clamp(totalBlocks / 15, 1, 1024);
		
		unsortedBlocks = new ArrayDeque<>(hack.getNonEmptyBlocks().keySet());
		
		BlockPos origin = hack.getOriginPos();
		sortingHelper = new TreeSet<>(
			Comparator.<BlockPos> comparingDouble(pos -> pos.distSqr(origin))
				.thenComparing(pos -> pos));
	}
	
	@Override
	public void onUpdate(TemplateToolHack hack)
	{
		// Pass 1: sort by distance from origin
		if(!unsortedBlocks.isEmpty())
		{
			for(int i = 0; i < blocksPerTick && !unsortedBlocks.isEmpty(); i++)
				sortingHelper.add(unsortedBlocks.removeLast());
			
			progress = sortingHelper.size() / (float)totalBlocks;
			return;
		}
		
		// Set the closest-to-origin block as the first block
		LinkedHashSet<BlockPos> sortedBlocks = hack.getSortedBlocks();
		if(sortedBlocks.isEmpty() && !sortingHelper.isEmpty())
		{
			BlockPos first = sortingHelper.first();
			sortedBlocks.add(first);
			sortingHelper.remove(first);
		}
		
		// Pass 2: walk from the first block until no blocks are left
		for(int i = 0; i < blocksPerTick && !sortingHelper.isEmpty(); i++)
		{
			BlockPos current = sortingHelper.first();
			double dCurrent = Double.MAX_VALUE;
			
			for(BlockPos pos : sortingHelper)
			{
				double dPos = sortedBlocks.getLast().distSqr(pos);
				if(dPos >= dCurrent)
					continue;
				
				for(Direction side : Direction.values())
				{
					BlockPos next = pos.relative(side);
					if(!sortedBlocks.contains(next))
						continue;
					
					current = pos;
					dCurrent = dPos;
				}
			}
			
			sortedBlocks.add(current);
			sortingHelper.remove(current);
		}
		
		progress = sortingHelper.size() / (float)totalBlocks;
		if(sortedBlocks.size() == totalBlocks)
			hack.setState(new ChooseNameState());
	}
	
	@Override
	public void onRender(TemplateToolHack hack, PoseStack matrixStack,
		float partialTicks)
	{
		int black = 0x80000000;
		int green30 = 0x4D00FF00;
		
		BlockPos start = hack.getStartPos();
		BlockPos end = hack.getEndPos();
		AABB bounds =
			AABB.encapsulatingFullBlocks(start, end).deflate(1 / 16.0);
		
		// Draw scanner
		double scannerX = Mth.lerp(progress, bounds.minX, bounds.maxX);
		AABB scanner = bounds.setMinX(scannerX).setMaxX(scannerX);
		RenderUtils.drawOutlinedBox(matrixStack, scanner, black, true);
		RenderUtils.drawSolidBox(matrixStack, scanner, green30, true);
		
		// Draw recently sorted blocks
		List<AABB> boxes = hack.getSortedBlocks().reversed().stream()
			.map(pos -> new AABB(pos).deflate(1 / 16.0)).limit(1024).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, black, false);
	}
	
	@Override
	protected String getMessage(TemplateToolHack hack)
	{
		return "Creating template...";
	}
}
