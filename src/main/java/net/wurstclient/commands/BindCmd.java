/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;

public final class BindCmd extends Command
{
	public BindCmd()
	{
		super("bind", "Shortcut for '.binds add'.", ".bind <key> <hacks>",
			".bind <key> <commands>",
			"Multiple hacks/commands must be separated by ';'.",
			"Use .binds for more options.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		WURST.getCmdProcessor().process("binds add " + String.join(" ", args));
	}
}
