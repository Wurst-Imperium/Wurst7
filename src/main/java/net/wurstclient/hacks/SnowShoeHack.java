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

@SearchTags({"snow shoe", "SnowJesus", "snow jesus", "NoSnowSink",
	"no snow sink", "AntiSnowSink", "anti snow sink"})
public final class SnowShoeHack extends Hack
{
	public SnowShoeHack()
	{
		super("SnowShoe", "Allows you to walk on powder snow.");
		setCategory(Category.MOVEMENT);
	}
}
