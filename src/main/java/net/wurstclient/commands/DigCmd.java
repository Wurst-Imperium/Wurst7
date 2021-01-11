/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;

public final class DigCmd extends Command
{
	public DigCmd()
	{
		super("dig",
			"Automatically digs out the selected area,\n"
				+ "starting in the front-left-top corner.",
			".dig <length> <width> <height>", ".dig stop");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 1 && args[0].equalsIgnoreCase("stop"))
			WURST.getHax().excavatorHack.setEnabled(false);
		else
			startDigging(args);
	}
	
	private void startDigging(String[] args) throws CmdSyntaxError
	{
		if(args.length != 3)
			throw new CmdSyntaxError();
		
		int length = tryParseInt(args[0], "length");
		int width = tryParseInt(args[1], "width");
		int height = tryParseInt(args[2], "height");
		
		ClientPlayerEntity player = MC.player;
		Direction direction = player.getHorizontalFacing();
		
		BlockPos pos1 = new BlockPos(
			player.getPos().add(0, player.getEyeHeight(player.getPose()), 0));
		
		if(height < 0)
			pos1 = pos1.down();
		
		BlockPos pos2 =
			pos1.offset(direction, length > 0 ? length - 1 : length + 1);
		
		pos2 = pos2.offset(direction.rotateYClockwise(),
			width > 0 ? width - 1 : width + 1);
		
		pos2 = pos2.down(height > 0 ? height - 1 : height + 1);
		
		WURST.getHax().excavatorHack.enableWithArea(pos1, pos2);
	}
	
	private int tryParseInt(String input, String name) throws CmdSyntaxError
	{
		int i;
		
		try
		{
			i = Integer.parseInt(input);
			
		}catch(NumberFormatException e)
		{
			throw new CmdSyntaxError("Invalid " + name + ": " + input);
		}
		
		if(i == 0)
			throw new CmdSyntaxError(name + " can't be zero");
		
		return i;
	}
}
