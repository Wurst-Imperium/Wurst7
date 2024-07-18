/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;

public final class XrayCmd extends Command
{
	public XrayCmd()
	{
		super("xray", "Shortcut for '.blocklist X-Ray Ores'.",
			".xray add <block>", ".xray remove <block>", ".xray list [<page>]",
			".xray reset", "Example: .xray add gravel");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		WURST.getCmdProcessor()
			.process("blocklist X-Ray Ores " + String.join(" ", args));
	}
}
