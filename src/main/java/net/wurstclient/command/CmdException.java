/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.command;

public abstract class CmdException extends Exception
{
	protected final Command cmd;
	
	public CmdException(Command cmd)
	{
		super();
		this.cmd = cmd;
	}
	
	public CmdException(Command cmd, String message)
	{
		super(message);
		this.cmd = cmd;
	}
	
	public abstract void printToChat();
}
