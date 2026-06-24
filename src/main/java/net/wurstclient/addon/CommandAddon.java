/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.addon;

import net.wurstclient.command.Command;

public interface CommandAddon
{
	/**
	 * Returns the name of this addon
	 */
	String getAddonName();
	
	/**
	 * Returns all commands provided by this addon
	 */
	Command[] getCommands();
}
