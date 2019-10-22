package net.wurstclient.commands;

import net.wurstclient.WurstClient;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public class FCmd extends Command
{
	public FCmd()
	{
		super("f", "List, add, or remove friends.",
			".f [list|add|remove] <playername>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1 || args.length > 2)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "list":
			ChatUtils.message("These are your friends:");
			for(String friend : WurstClient.INSTANCE.getFriendsList()
				.getAllFriends())
				ChatUtils.message(friend);
			break;
			case "add":
			if(args.length < 2)
				throw new CmdError("No player supplied!");
			WurstClient.INSTANCE.getFriendsList().addFriend(args[1]);
			break;
			case "remove":
			if(args.length < 2)
				throw new CmdError("No player supplied!");
			WurstClient.INSTANCE.getFriendsList().removeFriend(args[1]);
			break;
			default:
			throw new CmdError(
				"First parameter must be \"list\", \"add\", or \"remove\".");
		}
	}
}
