/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.BlinkReplayHack;

public final class BlinkReplayCmd extends Command
{
	public BlinkReplayCmd()
	{
		super("blinkreplay", "Triggers BlinkReplay", ".replay");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length > 1)
			throw new CmdSyntaxError();
		
		BlinkReplayHack blinkReplayHack = WURST.getHax().blinkReplayHack;
		
		if(args.length == 0)
		{
			blinkReplayHack.setEnabled(!blinkReplayHack.isEnabled());
			return;
		}
		
		switch(args[0].toLowerCase())
		{
			default:
			throw new CmdSyntaxError();
			
			case "on":
			blinkReplayHack.setEnabled(true);
			break;
			
			case "off":
			blinkReplayHack.setEnabled(false);
			break;
			
			case "cancel":
			cancel(blinkReplayHack);
			break;
		}
	}
	
	private void cancel(BlinkReplayHack blinkReplayHack) throws CmdException
	{
		if(!blinkReplayHack.isEnabled())
			throw new CmdError("Cannot cancel, BlinkReplay is not running!");
		
		blinkReplayHack.cancel();
	}
}
