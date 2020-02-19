/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.util.InputUtil;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.keybinds.Keybind;
import net.wurstclient.keybinds.KeybindList;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.MathUtils;

public final class BindsCmd extends Command
{
	public BindsCmd()
	{
		super("binds", "Allows you to manage keybinds through the chat.",
			".binds add <key> <hacks>", ".binds add <key> <commands>",
			".binds remove <key>", ".binds list [<page>]", ".binds remove-all",
			".binds reset",
			"Multiple hacks/commands must be separated by ';'.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		switch(args[0].toLowerCase())
		{
			case "add":
			add(args);
			break;
			
			case "remove":
			remove(args);
			break;
			
			case "list":
			list(args);
			break;
			
			case "remove-all":
			removeAll();
			break;
			
			case "reset":
			reset();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void add(String[] args) throws CmdException
	{
		if(args.length < 3)
			throw new CmdSyntaxError();
		
		String displayKey = args[1];
		String key = parseKey(displayKey);
		String[] cmdArgs = Arrays.copyOfRange(args, 2, args.length);
		String commands = String.join(" ", cmdArgs);
		
		WURST.getKeybinds().add(key, commands);
		ChatUtils.message("Keybind set: " + displayKey + " -> " + commands);
	}
	
	private void remove(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		String displayKey = args[1];
		String key = parseKey(displayKey);
		
		String commands = WURST.getKeybinds().getCommands(key);
		if(commands == null)
			throw new CmdError("Nothing to remove.");
		
		WURST.getKeybinds().remove(key);
		ChatUtils.message("Keybind removed: " + displayKey + " -> " + commands);
	}
	
	private String parseKey(String displayKey) throws CmdSyntaxError
	{
		String key = displayKey.toLowerCase();
		
		String prefix = "key.keyboard.";
		if(!key.startsWith(prefix))
			key = prefix + key;
		
		try
		{
			InputUtil.fromName(key);
			return key;
			
		}catch(IllegalArgumentException e)
		{
			throw new CmdSyntaxError("Unknown key: " + displayKey);
		}
	}
	
	private void list(String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		List<Keybind> binds = WURST.getKeybinds().getAllKeybinds();
		int page = parsePage(args);
		int pages = (int)Math.ceil(binds.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		String total = "Total: " + binds.size() + " keybind";
		total += binds.size() != 1 ? "s" : "";
		ChatUtils.message(total);
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, binds.size());
		
		ChatUtils.message("Keybind list (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(binds.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 2)
			return 1;
		
		if(!MathUtils.isInteger(args[1]))
			throw new CmdSyntaxError("Not a number: " + args[1]);
		
		return Integer.parseInt(args[1]);
	}
	
	private void removeAll()
	{
		WURST.getKeybinds().removeAll();
		ChatUtils.message("All keybinds removed.");
	}
	
	private void reset()
	{
		WURST.getKeybinds().setKeybinds(KeybindList.DEFAULT_KEYBINDS);
		ChatUtils.message("All keybinds reset to defaults.");
	}
}
