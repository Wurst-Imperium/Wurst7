/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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

@SearchTags({".legit", "dots in chat", "command bypass", "prefix"})
public final class SayCmd extends Command
{
	public SayCmd()
	{
		super("say",
			"Sends the given chat message, even if it starts with a\n" + "dot.",
			".say <message>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		String message = String.join(" ", args);
		if(message.startsWith("/"))
			MC.player.sendCommand(message.substring(1));
		else
			MC.player.sendChatMessage(message, null);
	}
}
