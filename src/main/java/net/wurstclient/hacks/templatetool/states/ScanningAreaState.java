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

import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
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
		blocksPerTick = MathHelper.clamp(totalBlocks / 30, 1, 1024);
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
			
			if(!state.isReplaceable())
				hack.getNonEmptyBlocks().put(pos, state);
		}
		
		progress = scannedBlocks / (float)totalBlocks;
		
		if(!iterator.hasNext())
			hack.setState(new SelectOriginState());
	}
	
	@Override
	public void onRender(TemplateToolHack hack, MatrixStack matrixStack,
		float partialTicks)
	{
		int black = 0x80000000;
		int green15 = 0x2600FF00;
		int green30 = 0x4D00FF00;
		
		// Draw recently scanned blocks
		LinkedHashMap<BlockPos, BlockState> blocks = hack.getNonEmptyBlocks();
		int offset = Math.max(0, blocks.size() - blocksPerTick);
		List<Box> boxes = blocks.keySet().stream().skip(offset)
			.map(pos -> new Box(pos).expand(0.005)).toList();
		
		RenderUtils.drawOutlinedBoxes(matrixStack, boxes, black, true);
		RenderUtils.drawSolidBoxes(matrixStack, boxes, green15, true);
		
		// Draw scanner
		BlockPos start = hack.getStartPos();
		BlockPos end = hack.getEndPos();
		Box bounds = Box.enclosing(start, end).contract(1 / 16.0);
		double scannerX = MathHelper.lerp(progress, bounds.minX, bounds.maxX);
		Box scanner = bounds.withMinX(scannerX).withMaxX(scannerX);
		
		RenderUtils.drawOutlinedBox(matrixStack, scanner, black, true);
		RenderUtils.drawSolidBox(matrixStack, scanner, green30, true);
	}
	
	@Override
	protected String getMessage(TemplateToolHack hack)
	{
		return "Scanning area...";
	}
}
