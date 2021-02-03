/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.RemoteViewHack;

public final class RvCmd extends Command
{
	public RvCmd()
	{
		super("rv", "Makes RemoteView target a specific entity.",
				CmdProcessor.getPrefix() + "rv <entity>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		RemoteViewHack remoteView = WURST.getHax().remoteViewHack;
		
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		remoteView.onToggledByCommand(args[0]);
	}
}
