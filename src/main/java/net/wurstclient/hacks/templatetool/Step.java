/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool;

import net.minecraft.util.math.BlockPos;

public enum Step
{
	START_POS("Select start position.", true),
	
	END_POS("Select end position.", true),
	
	SCAN_AREA("Scanning area...", false),
	
	FIRST_BLOCK("Select the first block to be placed by AutoBuild.", true),
	
	CREATE_TEMPLATE("Creating template...", false),
	
	FILE_NAME("Choose a name for this template.", false),
	
	SAVE_FILE("Saving file...", false);
	
	public static final Step[] SELECT_POSITION_STEPS =
		{START_POS, END_POS, FIRST_BLOCK};
	
	private final String message;
	private final boolean selectPos;
	
	private BlockPos pos;
	
	private Step(String message, boolean selectPos)
	{
		this.message = message;
		this.selectPos = selectPos;
	}
	
	public BlockPos getPos()
	{
		return pos;
	}
	
	public void setPos(BlockPos pos)
	{
		this.pos = pos;
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public boolean doesSelectPos()
	{
		return selectPos;
	}
}
