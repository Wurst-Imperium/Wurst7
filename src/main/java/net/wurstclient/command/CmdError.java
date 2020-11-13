/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.command;

import net.wurstclient.util.ChatUtils;

public final class CmdError extends CmdException
{
	public CmdError(String message)
	{
		super(message);
	}
	
	@Override
	public void printToChat(Command cmd)
	{
		ChatUtils.error(getMessage());
	}
}
