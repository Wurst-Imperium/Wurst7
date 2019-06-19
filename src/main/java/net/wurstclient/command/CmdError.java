/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.command;

import net.wurstclient.util.ChatUtils;

public final class CmdError extends CmdException
{
	public CmdError(Command cmd, String message)
	{
		super(cmd, message);
	}
	
	@Override
	public void printToChat()
	{
		ChatUtils.error(getMessage());
	}
}
