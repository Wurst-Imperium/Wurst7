/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.MathUtils;

public final class VClipCmd extends Command
{
	public VClipCmd()
	{
		super("vclip", "Lets you clip through blocks vertically.\n"
			+ "The maximum distance is 10 blocks.", ".vclip <height>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		if(!MathUtils.isInteger(args[0]))
			throw new CmdSyntaxError();
		
		ClientPlayerEntity player = MC.player;
		player.updatePosition(player.getX(),
			player.getY() + Integer.parseInt(args[0]), player.getZ());
	}
}
