/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.concurrent.TimeUnit;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.MathUtils;

public final class StopCmd extends Command
{
	public StopCmd()
	{
		super("stop", "Let your minecraft stop for a specific time\n", ".stop <time>");
	}
	@Override
	public void call(String[] args) throws InterruptedException, CmdException
	{	
		if(!MathUtils.isInteger(args[0]))
			throw new CmdSyntaxError();
		
		TimeUnit.SECONDS.sleep (Integer.parseInt(args[0]));
		
	}
}
