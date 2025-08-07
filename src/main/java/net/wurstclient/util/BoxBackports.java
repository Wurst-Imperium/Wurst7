/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public enum BoxBackports
{
	;
	
	public static Box enclosing(BlockPos pos1, BlockPos pos2)
	{
		return new Box(Math.min(pos1.getX(), pos2.getX()),
			Math.min(pos1.getY(), pos2.getY()),
			Math.min(pos1.getZ(), pos2.getZ()),
			Math.max(pos1.getX(), pos2.getX()) + 1,
			Math.max(pos1.getY(), pos2.getY()) + 1,
			Math.max(pos1.getZ(), pos2.getZ()) + 1);
	}
}
