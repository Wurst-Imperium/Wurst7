/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"name tags"})
public final class NameTagsHack extends Hack
{
	public NameTagsHack()
	{
		super("NameTags", "Changes the scale of the nametags so you can\n"
			+ "always read them.\n" + "Also allows you to see the nametags of\n"
			+ "sneaking players.");
		
		setCategory(Category.RENDER);
	}
}
