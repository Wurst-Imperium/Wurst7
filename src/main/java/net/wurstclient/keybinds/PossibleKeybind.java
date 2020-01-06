/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

public final class PossibleKeybind
{
	private final String command;
	private final String description;
	
	public PossibleKeybind(String command, String description)
	{
		this.command = command;
		this.description = description;
	}
	
	public String getCommand()
	{
		return command;
	}
	
	public String getDescription()
	{
		return description;
	}
}
