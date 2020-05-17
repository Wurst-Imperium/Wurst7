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
import net.wurstclient.hacks.BlinkHack;
import net.wurstclient.hacks.BlinkReplayHack;

public final class ReplayCmd extends Command
{
	public ReplayCmd()
	{
		super("replay", "Triggers BlinkReplay", ".replay");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		BlinkReplayHack blinkReplayHack = WURST.getHax().blinkReplayHack;
		
		if(args.length == 0)
		{
			enable(blinkReplayHack);
			return;
		}

		throw new CmdSyntaxError();
	}

	private void enable(BlinkReplayHack blinkReplayHack) throws CmdException
	{
		BlinkHack blinkHack = WURST.getHax().blinkHack;

		if(!blinkHack.isEnabled())
			throw new CmdError("Cannot trigger BlinkReplay without Blink enabled.");

		if(blinkReplayHack.isEnabled()) {
			blinkReplayHack.appendPackets();
			throw new CmdError("BlinkReplay already active, appending new packets...");
		}

		blinkReplayHack.setEnabled(true);
	}
	
	private void cancel(BlinkReplayHack blinkReplayHack) throws CmdException
	{
		if(!blinkReplayHack.isEnabled())
			throw new CmdError("Cannot cancel, BlinkReplay is not running!");
		
		blinkReplayHack.cancel();
	}
}
