package net.wurstclient.commands;

import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.Command;

public class FCmd extends Command {
	public FCmd() {
		super("f", "List, add, or remove friends.", ".f [list|add|remove] <playername>");
	}

	@Override
	public void call(String[] args) throws CmdException {
		if (args.length < 1 || args.length > 2) throw new CmdError("Incorrect number of parameters passed!");

		// TODO: Add a friends list.
		switch (args[0].toLowerCase())
		{
			case "list":
				System.out.println("LIST");
				break;
			case "add":
				System.out.println("ADD");
				break;
			case "remove":
				System.out.println("REMOVE");
				break;
			default:
				throw new CmdError("First parameter must be \"list\", \"add\", or \"remove\".");
		}
	}
}
