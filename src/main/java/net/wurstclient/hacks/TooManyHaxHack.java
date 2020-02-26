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

@SearchTags({"too many hax", "TooManyHacks", "too many hacks"})
public final class TooManyHaxHack extends Hack
{
	public TooManyHaxHack()
	{
		super("TooManyHax", "Hides and disables features that you don't want.\n"
			+ "For those who want to \"only hack a little bit\".\n\n"
			+ "Use the \u00a7l.toomanyhax\u00a7r command to choose which features to hide.\n"
			+ "Type \u00a7l.help toomanyhax\u00a7r for more info.");
		setCategory(Category.OTHER);
	}
}
