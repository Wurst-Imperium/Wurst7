/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.SpambotHack;

@SearchTags({"spambot", "chatspam", "spammer"})
public final class SpambotCmd extends Command {
	public SpambotCmd() {
		super("spam", "Sets the message for the Spambot hack to use.\n",
			".spam <message> \n\n"
			+ "You can use:\n"
			+ "%user% for your username\n"
			+ "%time% for the current desktop time\n"
			+ "%date% for the current desktop date\n"
			+ "%fulldate% for the both\n"
			+ "%rand% for a random number between 1 and 9999."
			+ "Good for bypassing spam filters.\n"
			+ "%ruser% for a random user. Nuff said.\n"
			+ "And finally, use 2 percent signs to actually put a % into chat.");
	}
	
	@Override
	public void call(String[] args) throws CmdException {
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		SpambotHack.message = String.join(" ", args);
	}
}
