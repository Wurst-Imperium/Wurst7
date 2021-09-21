/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;

@SearchTags({"auto reconnect", "AutoRejoin", "auto rejoin"})
@DontBlock
public final class AutoReconnectHack extends Hack
{
	public AutoReconnectHack()
	{
		super("AutoReconnect", "Automatically reconnects when you get\n"
			+ "kicked from the server.");
		setCategory(Category.OTHER);
	}
}
