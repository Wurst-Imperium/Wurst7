package net.wurstclient.commands;

import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

@SearchTags({"feed", "stand", "sit", "pet", "aura"})
public final class SitAuraCmd extends Command
{
	public SitAuraCmd()
	{
		super("SitAura", "Sits, stands, or feeds nearby pets",
			".sitaura <sit/stand/feed> <mob name>");
	}
	
	static String[] action = new String[]{"sit", "stand", "feed"};
	static String[] entity = new String[]{"wolf", "cat", "horse", "donkey",
		"mule", "llama", "parrot", "fox", "all"};
	
	@Override
	public void call(String[] args) throws CmdException
	{
		
		// Set Base Variables
		String ActionCheck = "";
		String EntityCheck = "";
		String Action = "";
		String Entity = "";
		Boolean ArgumentsRecieved = false;
		// Do Checks
		if(args.length < 2)
			throw new CmdSyntaxError(
				"Not Enough Arguments, Check Your Command");
		if(args.length > 2)
			throw new CmdSyntaxError("Too Many Arguments, Check Your Command");
		
		// Do Checks and Assign Action
		for(int i = 0; i < action.length; i++)
		{
			if(args[0].toString().equals(action[i].toString()))
			{
				Action = action[i];
				ActionCheck = "passed";
				break;
			}else
			{
				if(i == action.length - 1)
				{
					System.out.println("(Debug)Action Check Failed");
					System.out.println(args[0]);
					System.out.println(action[i]);
					ActionCheck = "failed";
				}else
					System.out.println("(Debug)still checking");
				
			}
		}
		
		// Do Checks and Assign Entity
		for(int j = 0; j < entity.length; j++)
		{
			if(args[1].toString().equals(entity[j].toString()))
			{
				Entity = entity[j];
				EntityCheck = "passed";
				break;
			}else
			{
				if(j == entity.length - 1)
				{
					System.out.println("(Debug)Entity Check Failed");
					System.out.println(args[1]);
					System.out.println(entity[j]);
					EntityCheck = "failed";
				}else
					System.out.println("(Debug) still checking");
			}
		}
		
		// Check Feedback
		while(ActionCheck != "" && EntityCheck != ""
			&& ArgumentsRecieved != true)
		{
			if(ActionCheck == "failed" && EntityCheck == "passed")
				throw new CmdSyntaxError(
					"Entity Check Passed, Check Your Action Argument");
			
			if(EntityCheck == "failed" && ActionCheck == "passed")
				throw new CmdSyntaxError(
					"Action Check Passed, Check Your Entity Argument");
			
			if(EntityCheck == "failed" && ActionCheck == "failed")
				throw new CmdSyntaxError(
					"Something Has Gone Horibly Wrong, Check Both Arguments");
			
			if(ActionCheck == "passed" && EntityCheck == "passed")
			{
				ChatUtils.message("Both Checks Passed! Action: " + Action
					+ ", Entity: " + Entity + ".");
				// Code To Do Action Here
			}
			ArgumentsRecieved = true;
		}
		
	}
}
// if(args[0]=="test")
// ChatUtils.message("I don't do anything yet, I'm supposed to do something to
// all mobs!");
// action.length
