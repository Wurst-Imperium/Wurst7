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

@SearchTags({"ghost hand"})
public final class GhostHandHack extends Hack
{
	public GhostHandHack()
	{
		super("GhostHand",
			"Allows you to reach specific blocks through walls.\n"
				+ "Type \u00a7l.ghosthand id <block_id>\u00a7r or \u00a7l.ghosthand name <block_name>\u00a7r to specify it.");
		setCategory(Category.BLOCKS);
	}
}
