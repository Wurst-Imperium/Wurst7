/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.OnInvFullHack;

public final class OnInvFullCmd extends Command {
	public OnInvFullCmd() {
		super("oninvfull", "Writes message when your inventory becomes full.", ".oninvfull <action>",
			"Example: .oninvfull /sellall",
			"Runs /sellall when inventory becomes full.",
			"Compatible with Wurst \"dot\" commands");
	}
	
	@Override
	public void call(String[] args) throws CmdException {
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		OnInvFullHack.action = String.join(" ", args);
	}
}
