/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import net.minecraft.core.BlockPos;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.SelectPositionState;
import net.wurstclient.hacks.templatetool.TemplateToolState;

public final class SelectOriginState extends SelectPositionState
{
	@Override
	protected String getDefaultMessage()
	{
		return "Select the first block to be placed by AutoBuild.";
	}
	
	@Override
	protected BlockPos getSelectedPos(TemplateToolHack hack)
	{
		return hack.getOriginPos();
	}
	
	@Override
	protected void setSelectedPos(TemplateToolHack hack, BlockPos pos)
	{
		hack.setOriginPos(pos);
	}
	
	@Override
	protected TemplateToolState getNextState()
	{
		return new CreatingTemplateState();
	}
}
