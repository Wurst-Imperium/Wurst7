/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.MathUtils;

public final class LookCmd extends Command
{
	public LookCmd()
	{
		super("look", "Allows you to set your yaw and pitch.\n"
			+ "Replace the value with skip if you do not intend to set the value.",
			".look <yaw> <pitch>");
	}

	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		if(!args[0].equalsIgnoreCase("skip"))
		{
			if(!MathUtils.isDouble(args[0]))
				throw new CmdSyntaxError("Yaw is not an integer!");
			float yaw = Float.parseFloat(args[0]);
			if(Math.abs(yaw) > 180)
				throw new CmdSyntaxError("Invaild yaw!");
			MC.player.setYaw(yaw);
		}
		if(!args[1].equalsIgnoreCase("skip"))
		{
			if(!MathUtils.isDouble(args[1]))
				throw new CmdSyntaxError("Pitch is not an integer!");
			float pitch = Float.parseFloat(args[1]);
			if(Math.abs(pitch) > 90)
				throw new CmdSyntaxError("Invaild pitch!");
			MC.player.setPitch(pitch);
		}
	}
}
