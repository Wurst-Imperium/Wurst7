/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class CmdAutoMaceToggle extends Command
{
	private static final WurstClient WURST = WurstClient.INSTANCE;
	
	public CmdAutoMaceToggle()
	{
		super("automace", "Toggle AutoMace on/off");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		WURST.getHax().autoMaceHack.toggleAutoMace();
		ChatUtils.message("AutoMace toggled.");
	}
}
