/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

public class Keybind
{
	private final String key;
	private final String commands;
	
	public Keybind(String key, String commands)
	{
		this.key = key;
		this.commands = commands;
	}
	
	public String getKey()
	{
		return key;
	}
	
	public String getCommands()
	{
		return commands;
	}
}
