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

@SearchTags({"no slowdown", "no slow down"})
public final class NoSlowdownHack extends Hack
{
	public NoSlowdownHack()
	{
		super("NoSlowdown", "Cancels slowness effects caused by\n"
			+ "honey, soul sand and using items.");
		setCategory(Category.MOVEMENT);
	}
}
