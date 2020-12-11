/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"no fire overlay"})
public final class NoFireOverlayHack extends Hack
{
	public NoFireOverlayHack()
	{
		super("NoFireOverlay",
			"Blocks the overlay when you are on fire.\n\n"
				+ "\u00a7c\u00a7lWARNING:\u00a7r This can cause you to burn\n"
				+ "to death without noticing.");
		
		setCategory(Category.RENDER);
	}
}
