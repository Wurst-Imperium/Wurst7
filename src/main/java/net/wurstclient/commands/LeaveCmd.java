/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.hacks.AutoLeaveHack;

public final class LeaveCmd extends Command
{
	public LeaveCmd()
	{
		super("leave", "Instantly disconnects from the server.",
			".leave [chars|tp|selfhurt|quit]");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
			disconnect();
		else if(args.length == 1)
			if(args[0].equalsIgnoreCase("taco"))
				for(int i = 0; i < 128; i++)
					MC.player.sendChatMessage("Taco!");
			else
				disconnectWithMode(parseMode(args[0]));
		else
			throw new CmdSyntaxError();
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Leave";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("leave");
	}
	
	public void disconnect()
	{
		disconnectWithMode(WURST.getHax().autoLeaveHack.mode.getSelected());
	}
	
	public void disconnectWithMode(AutoLeaveHack.Mode mode)
	{
		switch(mode)
		{
			case QUIT:
			MC.world.disconnect();
			break;
			
			case CHARS:
			MC.player.networkHandler
				.sendPacket(new ChatMessageC2SPacket("\u00a7"));
			break;
			
			case TELEPORT:
			MC.player.networkHandler
				.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(3.1e7,
					100, 3.1e7, false));
			break;
			
			case SELFHURT:
			MC.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket
				.attack(MC.player, MC.player.isSneaking()));
			break;
		}
	}
	
	private AutoLeaveHack.Mode parseMode(String input) throws CmdSyntaxError
	{
		// search mode by name
		AutoLeaveHack.Mode[] modes =
			WURST.getHax().autoLeaveHack.mode.getValues();
		for(int i = 0; i < modes.length; i++)
			if(input.equalsIgnoreCase(modes[i].toString()))
				return modes[i];
		
		// syntax error if mode does not exist
		throw new CmdSyntaxError("Invalid mode: " + input);
	}
}
