/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.command;

import net.wurstclient.util.ChatUtils;

public final class CmdSyntaxError extends CmdException
{
	public CmdSyntaxError()
	{
		super();
	}
	
	public CmdSyntaxError(String message)
	{
		super(message);
	}
	
	@Override
	public void printToChat(Command cmd)
	{
		String message = getMessage();
		if(message != null)
			ChatUtils.syntaxError(message);
		
		for(String line : cmd.getSyntax())
			ChatUtils.message(line);
	}
}
