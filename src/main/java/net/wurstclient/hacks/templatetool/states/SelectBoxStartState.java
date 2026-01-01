/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.SelectPositionState;
import net.wurstclient.hacks.templatetool.TemplateToolState;
import net.wurstclient.util.RenderUtils;

public final class SelectBoxStartState extends SelectPositionState
{
	@Override
	public void onRender(TemplateToolHack hack, PoseStack matrixStack,
		float partialTicks)
	{
		super.onRender(hack, matrixStack, partialTicks);
		
		BlockPos start = hack.getStartPos();
		if(start == null)
			return;
		
		AABB box = new AABB(start).deflate(1 / 16.0);
		int black = 0x80000000;
		int green15 = 0x2600FF00;
		RenderUtils.drawOutlinedBox(matrixStack, box, black, false);
		RenderUtils.drawSolidBox(matrixStack, box, green15, false);
	}
	
	@Override
	protected String getDefaultMessage()
	{
		return "Select start position.";
	}
	
	@Override
	protected BlockPos getSelectedPos(TemplateToolHack hack)
	{
		return hack.getStartPos();
	}
	
	@Override
	protected void setSelectedPos(TemplateToolHack hack, BlockPos pos)
	{
		hack.setStartPos(pos);
	}
	
	@Override
	protected TemplateToolState getNextState()
	{
		return new SelectBoxEndState();
	}
}
