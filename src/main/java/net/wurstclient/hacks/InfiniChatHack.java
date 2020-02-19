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

@SearchTags({"infini chat", "InfiniteChat", "infinite chat"})
public final class InfiniChatHack extends Hack
{
	public InfiniChatHack()
	{
		super("InfiniChat", "Removes the 256 character limit from the chat.\n"
			+ "Useful for long commands that modify NBT data.\n\n"
			+ "\u00a76\u00a7lNOTICE:\u00a7r Not recommended for talking to people.\n"
			+ "Most servers will cut messages to 256\n"
			+ "characters on their end.");
		setCategory(Category.CHAT);
	}
}
