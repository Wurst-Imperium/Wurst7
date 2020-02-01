/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"hand noclip", "hand no clip"})
public final class HandNoClipHack extends Hack
{
	public HandNoClipHack()
	{
		super("HandNoClip",
			"Allows you to reach specific blocks through walls.");
		
		setCategory(Category.BLOCKS);
	}
}
