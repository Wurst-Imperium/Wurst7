/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.command;

import java.io.IOException;
import java.util.Arrays;

import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.wurstclient.WurstClient;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.hacks.TooManyHaxHack;
import net.wurstclient.util.ChatUtils;

public final class CmdProcessor implements ChatOutputListener
{
	private static CmdList cmds;

	private static String prefix = CmdProcessorFile.readPrefix();

	public static void setPrefix(String newPrefix) {
		prefix = newPrefix;
		CmdProcessorFile.writePrefix(prefix);
		cmds.updateCmd();
	}

	public static String getPrefix(){
		return prefix;
	}
	
	public CmdProcessor(CmdList cmds)
	{
		this.cmds = cmds;
	}
	
	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		if(!WurstClient.INSTANCE.isEnabled())
			return;
		
		String message = event.getOriginalMessage().trim();
		if(!message.startsWith(prefix))
			return;
		
		event.cancel();
		process(message.substring(prefix.length()));
	}
	
	public void process(String input)
	{
		try
		{
			Command cmd = parseCmd(input);

			TooManyHaxHack tooManyHax =
				WurstClient.INSTANCE.getHax().tooManyHaxHack;
			if(tooManyHax.isEnabled() && tooManyHax.isBlocked(cmd))
			{
				ChatUtils.error(cmd.getName() + " is blocked by TooManyHax.");
				return;
			}
			
			runCmd(cmd, input);
			
		}catch(CmdNotFoundException e)
		{
			e.printToChat();
		}
	}
	
	private Command parseCmd(String input) throws CmdNotFoundException
	{
		String cmdName = input.split(" ")[0];
		Command cmd = cmds.getCmdByName(cmdName);
		
		if(cmd == null)
			throw new CmdNotFoundException(input);
		
		return cmd;
	}
	
	private void runCmd(Command cmd, String input)
	{
		String[] args = input.split(" ");
		args = Arrays.copyOfRange(args, 1, args.length);
		
		try
		{
			cmd.call(args);
			
		}catch(CmdException e)
		{
			e.printToChat(cmd);
			
		}catch(Throwable e)
		{
			CrashReport report = CrashReport.create(e, "Running Wurst command");
			CrashReportSection section = report.addElement("Affected command");
			section.add("Command input", () -> input);
			throw new CrashException(report);
		}
	}
	
	private static class CmdNotFoundException extends Exception
	{
		private final String input;
		
		public CmdNotFoundException(String input)
		{
			super();
			this.input = input;
		}
		
		public void printToChat()
		{
			String cmdName = input.split(" ")[0];
			ChatUtils.error("Unknown command: " + prefix + cmdName);
			
			StringBuilder helpMsg = new StringBuilder();
			
			if(input.startsWith("/"))
			{
				helpMsg.append("Use \"" + prefix + "say " + input + "\"");
				helpMsg.append(" to send it as a chat command.");
				
			}else
			{
				helpMsg.append("Type \"" + prefix + "help\" for a list of commands or ");
				helpMsg.append("\"" + prefix + "say " + prefix + input + "\"");
				helpMsg.append(" to send it as a chat message.");
			}
			
			ChatUtils.message(helpMsg.toString());
		}
	}
}
