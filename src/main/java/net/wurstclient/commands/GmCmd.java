/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;

public final class GmCmd extends Command
{
	public GmCmd()
	{
		super("gm", "Shortcut for /gamemode.", ".gm <gamemode>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		String args2 = String.join(" ", args);
		switch(args2)
		{
			case "s":
			case "0":
			args2 = "survival";
			break;
			
			case "c":
			case "1":
			args2 = "creative";
			break;
			
			case "a":
			case "2":
			args2 = "adventure";
			break;
			
			case "sp":
			case "3":
			args2 = "spectator";
			break;
		}
		
		String message = "/gamemode " + args2;
		MC.player.sendChatMessage(message);
	}
}
