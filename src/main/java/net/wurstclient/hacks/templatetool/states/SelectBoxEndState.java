/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.SelectPositionState;
import net.wurstclient.hacks.templatetool.TemplateToolState;
import net.wurstclient.util.RenderUtils;

public final class SelectBoxEndState extends SelectPositionState
{
	@Override
	public void onRender(TemplateToolHack hack, MatrixStack matrixStack,
		float partialTicks)
	{
		super.onRender(hack, matrixStack, partialTicks);
		
		BlockPos start = hack.getStartPos();
		BlockPos end = hack.getEndPos();
		List<Box> selections = Stream.of(start, end).filter(Objects::nonNull)
			.map(pos -> new Box(pos).contract(1 / 16.0)).toList();
		
		int black = 0x80000000;
		int green15 = 0x2600FF00;
		RenderUtils.drawOutlinedBoxes(matrixStack, selections, black, false);
		RenderUtils.drawSolidBoxes(matrixStack, selections, green15, false);
	}
	
	@Override
	protected String getDefaultMessage()
	{
		return "Select end position.";
	}
	
	@Override
	protected BlockPos getSelectedPos(TemplateToolHack hack)
	{
		return hack.getEndPos();
	}
	
	@Override
	protected void setSelectedPos(TemplateToolHack hack, BlockPos pos)
	{
		hack.setEndPos(pos);
	}
	
	@Override
	protected TemplateToolState getNextState()
	{
		return new ScanningAreaState();
	}
}
