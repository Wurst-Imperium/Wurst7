/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.TemplateToolState;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;

public final class ScanningAreaState extends TemplateToolState
{
	private int totalBlocks;
	private int blocksPerTick;
	private Iterator<BlockPos> iterator;
	private int scannedBlocks;
	private float progress;
	
	@Override
	public void onEnter(TemplateToolHack hack)
	{
		BlockPos start = hack.getStartPos();
		BlockPos end = hack.getEndPos();
		int lengthX = Math.abs(start.getX() - end.getX()) + 1;
		int lengthY = Math.abs(start.getY() - end.getY()) + 1;
		int lengthZ = Math.abs(start.getZ() - end.getZ()) + 1;
		totalBlocks = lengthX * lengthY * lengthZ;
		blocksPerTick = Mth.clamp(totalBlocks / 30, 1, 1024);
		iterator = BlockUtils.getAllInBox(start, end).iterator();
	}
	
	@Override
	public void onUpdate(TemplateToolHack hack)
	{
		for(int i = 0; i < blocksPerTick && iterator.hasNext(); i++)
		{
			scannedBlocks++;
			BlockPos pos = iterator.next();
			BlockState state = BlockUtils.getState(pos);
			
			if(!state.canBeReplaced())
				hack.getNonEmptyBlocks().put(pos, state);
		}
		
		progress = scannedBlocks / (float)totalBlocks;
		
		if(!iterator.hasNext())
			hack.setState(new SelectOriginState());
	}
	
	@Override
	public void onRender(TemplateToolHack hack, PoseStack matrixStack,
		float partialTicks)
	{
		int black = 0x80000000;
		int green15 = 0x2600FF00;
		int green30 = 0x4D00FF00;
		
		// Draw recently scanned blocks
		LinkedHashMap<BlockPos, BlockState> blocks = hack.getNonEmptyBlocks();
		int offset = Math.max(0, blocks.size() - blocksPerTick);
		List<AABB> boxes = blocks.keySet().stream().skip(offset)
			.map(pos -> new AABB(pos).inflate(0.005)).toList();
		
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, black, true);
		RenderUtils.drawSolidBoxes(matrixStack, boxes, green15, true);
		
		// Draw scanner
		BlockPos start = hack.getStartPos();
		BlockPos end = hack.getEndPos();
		AABB bounds =
			AABB.encapsulatingFullBlocks(start, end).deflate(1 / 16.0);
		double scannerX = Mth.lerp(progress, bounds.minX, bounds.maxX);
		AABB scanner = bounds.setMinX(scannerX).setMaxX(scannerX);
		
		RenderUtils.drawOutlinedBox(matrixStack, scanner, black, true);
		RenderUtils.drawSolidBox(matrixStack, scanner, green30, true);
	}
	
	@Override
	protected String getMessage(TemplateToolHack hack)
	{
		return "Scanning area...";
	}
}
